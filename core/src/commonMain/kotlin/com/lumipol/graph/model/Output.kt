package com.lumipol.graph.model

/** 출력 전용 축 식별(X + 두 Y축 + 오버레이(축 없음, 실값 표시용)). */
enum class ChartAxis { X, Y_PRIMARY, Y_SECONDARY, Y_OVERLAY }

data class NormalizedPoint(val x: Double, val y: Double) // 각 0.0~1.0
data class SeriesLayout(val id: String, val role: SeriesRole, val points: List<NormalizedPoint>)

data class AxisTick(val value: Double, val position: Double) // position 0.0~1.0
data class AxisTicksLayout(val axis: ChartAxis, val ticks: List<AxisTick>)

data class RefLineLayout(val axis: Axis, val position: Double, val label: String?) // position 0.0~1.0 (해당 Y축)
data class RefBandLayout(val axis: Axis, val lower: Double, val upper: Double)      // 0.0~1.0
data class MarkerLayout(val position: Double, val label: String?, val emphasis: Boolean) // X 위치 0.0~1.0

data class SeriesStat(val id: String, val min: Double, val max: Double, val avg: Double)
data class SegmentStat(val min: Double, val max: Double, val avg: Double, val count: Int)
data class Stats(val perSeries: List<SeriesStat>, val segments: List<SegmentStat>, val segmentSeriesId: String?)

data class LineChartLayout(
    val series: List<SeriesLayout>,
    val axisTicks: List<AxisTicksLayout>,
    val refLines: List<RefLineLayout>,
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

/** 도넛 한 조각. fraction은 0.0~1.0(12시 0, 시계방향). value=원본 크기. */
data class DonutSegmentLayout(
    val startFraction: Double,
    val sweepFraction: Double,
    val value: Double,
    val colorRole: DonutColorRole,
)

/** total=세그먼트 value 합(0이면 무데이터). */
data class DonutChartLayout(
    val segments: List<DonutSegmentLayout>,
    val total: Double,
)

/** 페이스 전처리 결과 — 정제된 라인 포인트 + 통계 + 고도 실루엣. */
data class PaceSeriesResult(
    val pace: List<Point>,          // y = paceSeconds/60(분), 다운샘플·아웃라이어 컷 적용
    val heart: List<Point>,         // 전 포인트, 결측 승계. 무데이터면 emptyList
    val cadence: List<Point>,       // 전 포인트, 결측 승계. 무데이터면 emptyList
    val altitudeArea: List<Point>?, // 다운샘플. 평지(≤0.5m) 또는 <2점이면 null
    val bestPaceSeconds: Double,    // 유효 최소, 없으면 0
    val validPaceCount: Int,
)
