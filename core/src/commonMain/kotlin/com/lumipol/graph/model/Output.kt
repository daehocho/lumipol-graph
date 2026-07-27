package com.lumipol.graph.model

import com.lumipol.graph.scale.AxisDomain

/** 출력 전용 축 식별(X + 두 Y축 + 오버레이(축 없음, 실값 표시용)). */
enum class ChartAxis { X, Y_PRIMARY, Y_SECONDARY, Y_OVERLAY }

/**
 * 축 도메인 출력 — 렌더러가 tick 두 점에서 선형관계를 역산(구 AxisScale)하지 않도록
 * 코어가 계산에 쓴 도메인을 그대로 내보낸다(경계 정책 §4-1 "역산 금지").
 * 값이 없는 Y축은 null. 픽셀→도메인 변환은 [AxisDomain.denormalize]로 한다.
 */
data class ChartDomains(
    val x: AxisDomain,
    val yPrimary: AxisDomain?,
    val ySecondary: AxisDomain?,
)

data class NormalizedPoint(val x: Double, val y: Double) // 각 0.0~1.0
data class SeriesLayout(val id: String, val role: SeriesRole, val points: List<NormalizedPoint>)

data class AxisTick(val value: Double, val position: Double) // position 0.0~1.0
data class AxisTicksLayout(val axis: ChartAxis, val ticks: List<AxisTick>)

data class RefBandLayout(val axis: Axis, val lower: Double, val upper: Double)      // 0.0~1.0
data class MarkerLayout(val position: Double, val label: String?, val emphasis: Boolean) // X 위치 0.0~1.0

data class SeriesStat(val id: String, val min: Double, val max: Double, val avg: Double)
data class SegmentStat(val min: Double, val max: Double, val avg: Double, val count: Int)
data class Stats(val perSeries: List<SeriesStat>, val segments: List<SegmentStat>, val segmentSeriesId: String?)

data class LineChartLayout(
    val series: List<SeriesLayout>,
    val axisTicks: List<AxisTicksLayout>,
    val refBands: List<RefBandLayout>,
    val markers: List<MarkerLayout>,
    val stats: Stats,
    val domains: ChartDomains,
) {
    // ObjC export는 기본 인자를 내보내지 않는다(DonutSegment 선례) — 기존 호출부(테스트 전용
    // 직접 생성) 보존용. 프로덕션 레이아웃은 항상 엔진이 만들며 이 폴백 도메인(0~1)을 쓰지 않는다.
    constructor(
        series: List<SeriesLayout>,
        axisTicks: List<AxisTicksLayout>,
        refBands: List<RefBandLayout>,
        markers: List<MarkerLayout>,
        stats: Stats,
    ) : this(series, axisTicks, refBands, markers, stats, ChartDomains(AxisDomain(0.0, 1.0), null, null))
}

data class NearestResult(val seriesId: String, val x: Double, val y: Double)

/** 막대 1칸. value=스플릿 평균 페이스(sec/unit, 시간가중).
 *  heightFraction·position은 0.0~1.0, 반전 축 — 값이 작을수록(빠를수록) 크다. */
data class BarLayout(
    val index: Int,
    val value: Double,
    val heightFraction: Double,
    val colorRole: BarColorRole,
    val isPartial: Boolean,
    val endMinutes: Int? = null, // 시간모드 버킷 끝 표시용 정수 분(반올림·최소1). 거리모드 null
)

/**
 * 막대 색 앵커 — 컬러맵의 기준 3값. 렌더러 2곳+소비 앱 2곳이 각자 재계산하던 축약
 * (docs/refactor/21-constants-diff.md §4, 4벌 복제·실사고 2회)의 단일 원본.
 * 규칙: 온전(비부분) 스플릿이 2개 이상이고 범위가 있으면 온전 스플릿만, 아니면 전체.
 * average는 런 총합 평균(있고 >0이면)을 우선하되 [fastest, slowest]로 클램프.
 */
data class BarColorAnchors(val fastest: Double, val slowest: Double, val average: Double)

data class BarChartLayout(
    val bars: List<BarLayout>,
    val yTicks: List<AxisTick>,
    val referenceLinePosition: Double?,
    val colorAnchors: BarColorAnchors?,
) {
    // ObjC export 기본 인자 소실 대응 — 기존 호출부 보존용(위 LineChartLayout과 동일 선례).
    constructor(
        bars: List<BarLayout>,
        yTicks: List<AxisTick>,
        referenceLinePosition: Double?,
    ) : this(bars, yTicks, referenceLinePosition, null)
}

/**
 * 도넛 한 조각. fraction은 0.0~1.0(12시 0, 시계방향). value=원본 크기.
 * [sourceIndex]는 **원본 `DonutChartData.segments` 인덱스** — 엔진이 value<=0을 필터하므로
 * 레이아웃 인덱스와 어긋날 수 있고, 렌더러 히트테스트는 이 값을 그대로 보고해야 한다.
 */
data class DonutSegmentLayout(
    val startFraction: Double,
    val sweepFraction: Double,
    val value: Double,
    val colorRole: DonutColorRole,
    val sourceIndex: Int = -1,
    val label: String? = null,
)

/** total=세그먼트 value 합(0이면 무데이터). */
data class DonutChartLayout(
    val segments: List<DonutSegmentLayout>,
    val total: Double,
)

/**
 * 페이스 전처리 결과 — 정제된 라인 포인트 + 통계 + 고도 실루엣.
 *
 * 네 시리즈 필드는 [availableSeries]와 항상 일치한다: 지표가 가용하지 않으면 필드도
 * 비어 있고(고도는 null), 가용하면 비어 있지 않다. 둘 중 어느 쪽을 봐도 같은 답이 나온다.
 */
data class PaceSeriesResult(
    val pace: List<Point>,          // y = paceSeconds/60(분), 아웃라이어 컷·평활·다운샘플. 미가용이면 emptyList
    val heart: List<Point>,         // 전 포인트, 결측 승계(앞쪽 결측은 첫 유효값 소급). 미가용이면 emptyList
    val cadence: List<Point>,       // 전 포인트, 결측 승계(앞쪽 결측은 첫 유효값 소급). 미가용이면 emptyList
    val altitudeArea: List<Point>?, // 다운샘플. 고도 미측정이거나 <2점이면 null. 평지여도 측정됐으면 반환
    val bestPaceSeconds: Double,    // 평활·다운샘플된(표시되는) 페이스의 최소(초), 없으면 0 — 선과 일치. 미가용이어도 낸다
    val validPaceCount: Int,        // 필터·아웃라이어 통과 표본 수(다운샘플 이전). 위와 동일
    /**
     * 실제로 표시 가능한 지표 id 집합([com.lumipol.graph.PaceSeriesId]). 소비 앱의 지표 칩·범례
     * 노출과 [com.lumipol.graph.SeriesSelection] 입력의 단일 소스 — 앱이 개별 필드를 보고
     * 각자 판정하면 플랫폼마다 규칙이 갈리므로 코어가 확정해 내보낸다.
     */
    val availableSeries: Set<Int>,
)

/** 존 표시용 bpm 경계. upper=null이면 상한 없음(최대존). */
data class ZoneBpmRange(val lower: Int, val upper: Int?)
