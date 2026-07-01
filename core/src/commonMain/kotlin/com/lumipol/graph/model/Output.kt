package com.lumipol.graph.model

/** 출력 전용 축 식별(X + 두 Y축). */
enum class ChartAxis { X, Y_PRIMARY, Y_SECONDARY }

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
