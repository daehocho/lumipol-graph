package com.lumipol.graph.model

/** 출력 전용 축 식별(X + 두 Y축 + 오버레이(축 없음, 실값 표시용)). */
enum class ChartAxis { X, Y_PRIMARY, Y_SECONDARY, Y_OVERLAY }

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
)

data class NearestResult(val seriesId: String, val x: Double, val y: Double)

/** 막대 1칸. value=스플릿 평균 페이스(sec/unit, 시간가중). heightFraction·position은 0.0~1.0. */
data class BarLayout(
    val index: Int,
    val value: Double,
    val heightFraction: Double,
    val colorRole: BarColorRole,
    val isPartial: Boolean,
    val endMinutes: Int? = null, // 시간모드 버킷 끝 표시용 정수 분(반올림·최소1). 거리모드 null
)

data class BarChartLayout(
    val bars: List<BarLayout>,
    val yTicks: List<AxisTick>,
    val referenceLinePosition: Double?,
)

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
    val pace: List<Point>,          // y = paceSeconds/60(분), 다운샘플·아웃라이어 컷. 미가용이면 emptyList
    val heart: List<Point>,         // 전 포인트, 결측 승계(앞쪽 결측은 첫 유효값 소급). 미가용이면 emptyList
    val cadence: List<Point>,       // 전 포인트, 결측 승계(앞쪽 결측은 첫 유효값 소급). 미가용이면 emptyList
    val altitudeArea: List<Point>?, // 다운샘플. 고도 미측정이거나 <2점이면 null. 평지여도 측정됐으면 반환
    val bestPaceSeconds: Double,    // 유효 최소, 없으면 0. 페이스 미가용이어도 집계값은 그대로 낸다
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
