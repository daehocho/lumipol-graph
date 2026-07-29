package com.lumipol.graph

import com.lumipol.graph.model.*
import com.lumipol.graph.query.interpolatedY as interpolatedYQuery
import com.lumipol.graph.query.nearest as nearestQuery
import com.lumipol.graph.query.nearestScrub as nearestScrubQuery
import com.lumipol.graph.scale.AxisDomain
import com.lumipol.graph.scale.NiceScale
import com.lumipol.graph.scale.Y_AXIS_HEADROOM_FRACTION
import com.lumipol.graph.scale.niceScale
import com.lumipol.graph.scale.yValues
import com.lumipol.graph.stats.segmentStats
import com.lumipol.graph.stats.seriesStat

object LineChartEngine {

    fun layout(data: LineChartData): LineChartLayout = layout(data, backgroundArea = null)

    /**
     * 배경 area(고도 등) 인식 layout. 시리즈 없이 area만 있는 기록은 X 도메인을 area x범위로 잡는다
     * (시리즈가 없으면 X 도메인이 0~1로 붕괴해 렌더러 좌표계가 어긋난다 — 플랫폼 중립 규칙이므로
     * 코어가 책임진다). [backgroundArea]는 **x 오름차순** 전제. area가 퇴화(2점 미만·폭 0)면
     * 일반 규칙으로 폴백. area가 유효하고 SECONDARY 축이 비어 있으면 고도 눈금
     * ([ChartAxis.Y_OVERLAY])도 방출한다(0.40.0).
     *
     * [areaMinValueSpan]은 고도 눈금 위치의 분모 하한 — 렌더러가 실루엣에 쓰는
     * `ChartStyle.areaMinValueSpan`과 **같은 값**을 넘겨야 라벨-실루엣 정렬 불변식이 유지된다
     * (기본값만 믿으면 앱이 스타일을 오버라이드했을 때 라벨이 실루엣에서 떨어진다).
     */
    fun layout(
        data: LineChartData,
        backgroundArea: List<Point>?,
        areaMinValueSpan: Double = ChartDefaults.AREA_MIN_VALUE_SPAN,
    ): LineChartLayout {
        if (data.series.isEmpty() && backgroundArea != null && backgroundArea.size >= 2) {
            val first = backgroundArea.first()
            val last = backgroundArea.last()
            if (last.x > first.x) return layout(data, first.x, last.x, backgroundArea, areaMinValueSpan)
        }
        val maxTicks = data.config.maxTicks
        // X 도메인 (모든 시리즈 점의 x) — min은 nice 경계로 내리되 max는 데이터 끝에 맞춘다.
        // 도메인 축(X)까지 nice 올림하면 데이터 뒤로 빈 구간이 생겨(예: 10.06km → 15km) 플롯 폭을 낭비한다.
        val xs = data.series.flatMap { it.points }.map { it.x }
        val xNice = niceScale(xs.minOrNull() ?: 0.0, xs.maxOrNull() ?: 1.0, maxTicks)
        val xMax = xs.maxOrNull() ?: xNice.niceMax
        val xDom = AxisDomain(xNice.niceMin, xMax)
        val xTicks = xNice.ticks.filter { it <= xMax + xNice.step * 1e-6 }
        return layout(data, xDom, xTicks, windowed = false, backgroundArea, areaMinValueSpan)
    }

    /** [xMin, xMax] 구간만 보이는 viewport layout — X 도메인은 구간 그대로,
     *  Y 도메인·tick은 보이는 값 기준으로 재계산. 확대/팬 커밋 시 렌더러가 호출한다.
     *  창 폭 0/역전은 데이터(제스처) 유래 입력이라 예외 대신 전체 레이아웃으로 폴백한다 —
     *  ObjC 경계는 Kotlin 예외를 잡을 수 없어 iOS에서 크래시가 된다(경계 정책 §4-2). */
    fun layout(data: LineChartData, xMin: Double, xMax: Double): LineChartLayout =
        layout(data, xMin, xMax, backgroundArea = null)

    /** 줌 창 + 배경 area — 고도 눈금([ChartAxis.Y_OVERLAY])은 줌과 무관하게 전체 정규화 기준
     *  (실루엣이 창과 무관하게 자체 min~max 정규화를 유지하는 것과 정합).
     *  [areaMinValueSpan]은 area-인식 전체 layout과 동일한 계약. */
    fun layout(
        data: LineChartData,
        xMin: Double,
        xMax: Double,
        backgroundArea: List<Point>?,
        areaMinValueSpan: Double = ChartDefaults.AREA_MIN_VALUE_SPAN,
    ): LineChartLayout {
        if (!(xMax > xMin)) return layout(data, backgroundArea, areaMinValueSpan)
        val xNice = niceScale(xMin, xMax, data.config.maxTicks)
        val eps = xNice.step * 1e-6
        val xTicks = xNice.ticks.filter { it >= xMin - eps && it <= xMax + eps }
        return layout(data, AxisDomain(xMin, xMax), xTicks, windowed = true, backgroundArea, areaMinValueSpan)
    }

    private fun layout(
        data: LineChartData,
        xDom: AxisDomain,
        xTicks: List<Double>,
        windowed: Boolean,
        backgroundArea: List<Point>?,
        areaMinValueSpan: Double,
    ): LineChartLayout {
        // 시리즈별 가시 포인트 — windowed면 창 안 + 양쪽 이웃 1개(선이 화면 밖으로 이어지게)
        val visibleBySeries: Map<String, List<Point>> = data.series.associate { s ->
            s.id to if (windowed) visiblePoints(s.points, xDom) else s.points
        }

        // Y 도메인 (축별). 값이 없는 축은 null. windowed면 창 안 포인트만(이웃 제외) 반영.
        val yWindow = if (windowed) xDom else null
        val yNice: Map<Axis, NiceScale?> = Axis.entries.associateWith { axis ->
            val vals = yValues(data, axis, yWindow)
            if (vals.isEmpty()) null else niceScale(vals.min(), vals.max(), data.config.maxTicks, Y_AXIS_HEADROOM_FRACTION)
        }
        val yDom: Map<Axis, AxisDomain> = yNice.mapValues { (_, ns) ->
            if (ns == null) AxisDomain(0.0, 1.0) else AxisDomain(ns.niceMin, ns.niceMax)
        }

        // 시리즈 정규화 (이웃 포인트는 0..1 밖 좌표가 될 수 있고 렌더러가 클리핑)
        // OVERLAY 역할은 축 도메인과 무관하게 자체 min~max로 y를 0..1 정규화한다.
        val seriesLayout = data.series.map { s ->
            val points = visibleBySeries.getValue(s.id)
            val normPoints = if (s.role == SeriesRole.OVERLAY) {
                val ys = s.points.map { it.y }
                val selfDom = AxisDomain(ys.minOrNull() ?: 0.0, ys.maxOrNull() ?: 1.0)
                points.map { NormalizedPoint(xDom.normalize(it.x), selfDom.normalize(it.y)) }
            } else {
                val dom = yDom.getValue(s.axis)
                points.map { NormalizedPoint(xDom.normalize(it.x), dom.normalize(it.y)) }
            }
            SeriesLayout(
                id = s.id,
                role = s.role,
                points = normPoints,
                axis = s.axis,
            )
        }

        // 축 tick: 어떤 출력 요소(시리즈/밴드)든 참조하는 축은 항상 여기 등장한다 —
        // refBand의 값도 yValues()에 흡수되어 해당 축의 도메인+틱을 만들어내기 때문.
        val axisTicks = buildList {
            add(AxisTicksLayout(ChartAxis.X, xTicks.map { AxisTick(it, xDom.normalize(it)) }))
            yNice[Axis.PRIMARY]?.let { ns ->
                add(AxisTicksLayout(ChartAxis.Y_PRIMARY, ns.ticks.map { AxisTick(it, yDom.getValue(Axis.PRIMARY).normalize(it)) }))
            }
            yNice[Axis.SECONDARY]?.let { ns ->
                add(AxisTicksLayout(ChartAxis.Y_SECONDARY, ns.ticks.map { AxisTick(it, yDom.getValue(Axis.SECONDARY).normalize(it)) }))
            }
            // Y_OVERLAY 게이트는 **전체 데이터 기준**(창 기준이면 SECONDARY 공백 구간으로 줌할 때
            // 고도 눈금이 제스처 중 깜빡이며 등장하고, "SECONDARY가 있는 차트엔 Y_OVERLAY가 오지
            // 않는다"는 앱 포매터 가정이 깨진다). yValues(xWindow=null) 공집합 판정과 동일 의미의
            // 구조 검사 — 제스처 커밋 경로라 리스트 생성 없이 단락 평가한다.
            val secondaryFree = data.series.none {
                it.axis == Axis.SECONDARY && it.role != SeriesRole.OVERLAY && it.points.isNotEmpty()
            } && data.referenceBands.none { it.axis == Axis.SECONDARY }
            overlayAxisTicks(backgroundArea, secondaryFree, areaMinValueSpan)?.let { add(it) }
        }

        // 밴드/마커 (마커는 windowed면 창 밖 제거)
        val refBands = data.referenceBands.map {
            val dom = yDom.getValue(it.axis)
            RefBandLayout(it.axis, dom.normalize(it.lower), dom.normalize(it.upper))
        }
        val markers = data.segmentMarkers
            .map { MarkerLayout(xDom.normalize(it.x), it.label, it.emphasis) }
            .filter { !windowed || it.position in 0.0..1.0 }

        // 통계: viewport 무관 — perSeries 전체, segments 는 첫 MAIN/PRIMARY 시리즈 기준
        val perSeries = data.series.map { seriesStat(it) }
        val splitBase = data.series.firstOrNull { it.role == SeriesRole.MAIN && it.axis == Axis.PRIMARY }
            ?: data.series.firstOrNull()
        val segments = splitBase?.let { segmentStats(it, data.config.segmentCount) } ?: emptyList()

        return LineChartLayout(
            series = seriesLayout,
            axisTicks = axisTicks,
            refBands = refBands,
            markers = markers,
            stats = Stats(perSeries, segments, if (segments.isEmpty()) null else splitBase?.id),
            // 계산에 실제로 쓴 도메인을 그대로 출력 — 값 없는 Y축은 null(폴백 0~1 도메인을 노출하지 않는다)
            domains = ChartDomains(
                x = xDom,
                yPrimary = if (yNice[Axis.PRIMARY] != null) yDom.getValue(Axis.PRIMARY) else null,
                ySecondary = if (yNice[Axis.SECONDARY] != null) yDom.getValue(Axis.SECONDARY) else null,
            ),
        )
    }

    /**
     * 고도 실루엣 눈금(0.40.0) — 고도가 축 슬롯 없이도 값 범위를 읽을 수 있게, SECONDARY 축이
     * 비어 있을 때만 min/max 2눈금을 낸다(렌더러 관례상 오른쪽). position은 **밴드 내 fraction**
     * (0=플롯 바닥, 1=밴드 상단 — 렌더러가 areaHeightFraction을 곱해 환산). 분모 하한
     * [minValueSpan]은 실루엣(heightFractions minSpan)과 같은 값이어야 위치가 실루엣과 항상
     * 정렬된다 — 렌더러가 `ChartStyle.areaMinValueSpan`을 그대로 넘긴다. 평지(min==max)는
     * 겹침 방지로 1눈금.
     */
    private fun overlayAxisTicks(
        backgroundArea: List<Point>?,
        secondaryFree: Boolean,
        minValueSpan: Double,
    ): AxisTicksLayout? {
        if (!secondaryFree || backgroundArea == null || backgroundArea.size < 2) return null
        // 제스처 커밋마다 불리는 경로 — 중간 리스트 없이 1패스 min/max.
        var lo = backgroundArea[0].y
        var hi = lo
        for (p in backgroundArea) {
            val y = p.y
            if (y < lo) lo = y
            if (y > hi) hi = y
        }
        val ticks = if (hi > lo) {
            listOf(AxisTick(lo, 0.0), AxisTick(hi, (hi - lo) / maxOf(hi - lo, minValueSpan)))
        } else {
            listOf(AxisTick(lo, 0.0))
        }
        return AxisTicksLayout(ChartAxis.Y_OVERLAY, ticks)
    }

    /** 창 안 포인트 + 양쪽 이웃 1개. 창이 포인트 사이 틈이면 가로지르는 이웃 쌍을 반환. */
    private fun visiblePoints(points: List<Point>, dom: AxisDomain): List<Point> {
        if (points.isEmpty()) return points
        val firstInside = points.indexOfFirst { it.x >= dom.min }
        val lastInside = points.indexOfLast { it.x <= dom.max }
        if (firstInside == -1 || lastInside < firstInside) {
            // 창 안에 포인트 없음 — 창을 가로지르는 이웃 쌍이 있으면 그 두 점
            val after = points.indexOfFirst { it.x > dom.max }
            return if (after > 0) points.subList(after - 1, after + 1) else emptyList()
        }
        val lo = maxOf(0, firstInside - 1)
        val hi = minOf(points.lastIndex, lastInside + 1)
        return points.subList(lo, hi + 1)
    }

    /** [x]는 원시 데이터-도메인 단위(0..1 정규화 아님) — 렌더러는 터치 위치를 원시 x로 변환한 뒤 호출해야 한다. */
    @Deprecated(
        "스냅 소스 선택·창 필터·정규화 좌표를 렌더러가 재구성해야 한다 — 코어가 확정하는 nearestScrub로 대체(B2)",
        ReplaceWith("nearestScrub(data, layout, x)"),
    )
    fun nearest(data: LineChartData, x: Double): List<NearestResult> = nearestQuery(data, x)

    /** 표시 창 [xMin, xMax] 안 점만 고려하는 근접 질의 — 줌 상태 스크럽용. */
    @Deprecated(
        "스냅 소스 선택·창 필터·정규화 좌표를 렌더러가 재구성해야 한다 — 코어가 확정하는 nearestScrub로 대체(B2)",
        ReplaceWith("nearestScrub(data, layout, x)"),
    )
    fun nearest(data: LineChartData, x: Double, xMin: Double, xMax: Double): List<NearestResult> =
        nearestQuery(data, x, xMin, xMax)

    /**
     * 스크럽 근접 질의 — 창 필터(도메인 ± [com.lumipol.graph.query.SCRUB_WINDOW_EPSILON])·
     * 스냅 소스 선택(main 우선)·정규화 좌표 산출까지 코어가 확정한다(B2).
     * [layout]은 같은 [data]로 만든 현재 표시 레이아웃(줌 창이면 창 layout).
     */
    fun nearestScrub(data: LineChartData, layout: LineChartLayout, x: Double): ScrubResult? =
        nearestScrubQuery(data, layout, x)

    /** 배경 area(고도 등) 스크럽 실값 — x 오름차순 [points]의 [x] 위치 y를 선형 보간(범위 밖 클램프). */
    fun interpolatedY(points: List<Point>, x: Double): Double? = interpolatedYQuery(points, x)
}
