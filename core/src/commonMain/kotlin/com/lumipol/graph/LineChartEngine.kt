package com.lumipol.graph

import com.lumipol.graph.model.*
import com.lumipol.graph.query.nearest as nearestQuery
import com.lumipol.graph.scale.AxisDomain
import com.lumipol.graph.scale.NiceScale
import com.lumipol.graph.scale.niceScale
import com.lumipol.graph.scale.yValues
import com.lumipol.graph.stats.segmentStats
import com.lumipol.graph.stats.seriesStat

object LineChartEngine {

    fun layout(data: LineChartData): LineChartLayout {
        val maxTicks = data.config.maxTicks
        // X 도메인 (모든 시리즈 점의 x) — min은 nice 경계로 내리되 max는 데이터 끝에 맞춘다.
        // 도메인 축(X)까지 nice 올림하면 데이터 뒤로 빈 구간이 생겨(예: 10.06km → 15km) 플롯 폭을 낭비한다.
        val xs = data.series.flatMap { it.points }.map { it.x }
        val xNice = niceScale(xs.minOrNull() ?: 0.0, xs.maxOrNull() ?: 1.0, maxTicks)
        val xMax = xs.maxOrNull() ?: xNice.niceMax
        val xDom = AxisDomain(xNice.niceMin, xMax)
        val xTicks = xNice.ticks.filter { it <= xMax + xNice.step * 1e-6 }
        return layout(data, xDom, xTicks, windowed = false)
    }

    /** [xMin, xMax] 구간만 보이는 viewport layout — X 도메인은 구간 그대로,
     *  Y 도메인·tick은 보이는 값 기준으로 재계산. 확대/팬 커밋 시 렌더러가 호출한다. */
    fun layout(data: LineChartData, xMin: Double, xMax: Double): LineChartLayout {
        require(xMax > xMin) { "xMax must be > xMin" }
        val xNice = niceScale(xMin, xMax, data.config.maxTicks)
        val eps = xNice.step * 1e-6
        val xTicks = xNice.ticks.filter { it >= xMin - eps && it <= xMax + eps }
        return layout(data, AxisDomain(xMin, xMax), xTicks, windowed = true)
    }

    private fun layout(
        data: LineChartData,
        xDom: AxisDomain,
        xTicks: List<Double>,
        windowed: Boolean,
    ): LineChartLayout {
        // 시리즈별 가시 포인트 — windowed면 창 안 + 양쪽 이웃 1개(선이 화면 밖으로 이어지게)
        val visibleBySeries: Map<String, List<Point>> = data.series.associate { s ->
            s.id to if (windowed) visiblePoints(s.points, xDom) else s.points
        }

        // Y 도메인 (축별). 값이 없는 축은 null. windowed면 창 안 포인트만(이웃 제외) 반영.
        val yWindow = if (windowed) xDom else null
        val yNice: Map<Axis, NiceScale?> = Axis.entries.associateWith { axis ->
            val vals = yValues(data, axis, yWindow)
            if (vals.isEmpty()) null else niceScale(vals.min(), vals.max(), data.config.maxTicks)
        }
        val yDom: Map<Axis, AxisDomain> = yNice.mapValues { (_, ns) ->
            if (ns == null) AxisDomain(0.0, 1.0) else AxisDomain(ns.niceMin, ns.niceMax)
        }

        // 시리즈 정규화 (이웃 포인트는 0..1 밖 좌표가 될 수 있고 렌더러가 클리핑)
        val seriesLayout = data.series.map { s ->
            val dom = yDom.getValue(s.axis)
            SeriesLayout(
                id = s.id,
                role = s.role,
                points = visibleBySeries.getValue(s.id)
                    .map { NormalizedPoint(xDom.normalize(it.x), dom.normalize(it.y)) },
            )
        }

        // 축 tick: 어떤 출력 요소(시리즈/기준선/밴드)든 참조하는 축은 항상 여기 등장한다 —
        // refLine/refBand의 값도 yValues()에 흡수되어 해당 축의 도메인+틱을 만들어내기 때문.
        val axisTicks = buildList {
            add(AxisTicksLayout(ChartAxis.X, xTicks.map { AxisTick(it, xDom.normalize(it)) }))
            yNice[Axis.PRIMARY]?.let { ns ->
                add(AxisTicksLayout(ChartAxis.Y_PRIMARY, ns.ticks.map { AxisTick(it, yDom.getValue(Axis.PRIMARY).normalize(it)) }))
            }
            yNice[Axis.SECONDARY]?.let { ns ->
                add(AxisTicksLayout(ChartAxis.Y_SECONDARY, ns.ticks.map { AxisTick(it, yDom.getValue(Axis.SECONDARY).normalize(it)) }))
            }
        }

        // 기준선/밴드/마커 (마커는 windowed면 창 밖 제거)
        val refLines = data.referenceLines.map {
            RefLineLayout(it.axis, yDom.getValue(it.axis).normalize(it.value), it.label)
        }
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
            refLines = refLines,
            refBands = refBands,
            markers = markers,
            stats = Stats(perSeries, segments, if (segments.isEmpty()) null else splitBase?.id),
        )
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
    fun nearest(data: LineChartData, x: Double): List<NearestResult> = nearestQuery(data, x)
}
