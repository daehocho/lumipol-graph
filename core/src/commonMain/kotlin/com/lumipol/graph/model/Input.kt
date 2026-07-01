package com.lumipol.graph.model

/** 시리즈가 붙는 Y축 선택 (이중 축 지원). */
enum class Axis { PRIMARY, SECONDARY }

/** 시리즈 역할 — 렌더링 스타일 힌트(주선 vs 흐린 고스트). */
enum class SeriesRole { MAIN, GHOST }

data class Point(val x: Double, val y: Double)

data class Series(
    val id: String,
    val points: List<Point>,
    val axis: Axis = Axis.PRIMARY,
    val role: SeriesRole = SeriesRole.MAIN,
)

/** 수평 기준선(목표 페이스 등). label은 앱이 이미 포맷한 문자열. */
data class RefLine(val value: Double, val axis: Axis = Axis.PRIMARY, val label: String? = null)

data class RefBand(val lower: Double, val upper: Double, val axis: Axis = Axis.PRIMARY)

/** X축 위 마커(km 구분 등). */
data class Marker(val x: Double, val label: String? = null, val emphasis: Boolean = false)

/** segmentCount: 구간 스플릿 통계를 낼 등간격 X 구간 수(0 = 스플릿 없음). */
data class ChartConfig(val segmentCount: Int = 0, val maxTicks: Int = 5)

data class LineChartData(
    val series: List<Series>,
    val referenceLines: List<RefLine> = emptyList(),
    val referenceBands: List<RefBand> = emptyList(),
    val segmentMarkers: List<Marker> = emptyList(),
    val config: ChartConfig = ChartConfig(),
)
