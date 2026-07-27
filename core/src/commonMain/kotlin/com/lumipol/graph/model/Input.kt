package com.lumipol.graph.model

/** 시리즈가 붙는 Y축 선택 (이중 축 지원). */
enum class Axis { PRIMARY, SECONDARY }

/** 시리즈 역할 — 렌더링 스타일 힌트(주선 vs 축 없는 정규화 오버레이). */
enum class SeriesRole { MAIN, OVERLAY }

data class Point(val x: Double, val y: Double)

data class Series(
    val id: String,
    val points: List<Point>,
    val axis: Axis = Axis.PRIMARY,
    val role: SeriesRole = SeriesRole.MAIN,
) {
    // ObjC export는 기본 인자를 내보내지 않는다 — iOS가 코어 기본값(PRIMARY/MAIN)을 상속하도록
    // 축약 생성자를 명시 제공(DonutSegment 선례, 브릿지 감사 §3).
    constructor(id: String, points: List<Point>) : this(id, points, Axis.PRIMARY, SeriesRole.MAIN)
}

data class RefBand(val lower: Double, val upper: Double, val axis: Axis = Axis.PRIMARY)

/** X축 위 마커(km 구분 등). */
data class Marker(val x: Double, val label: String? = null, val emphasis: Boolean = false)

/** segmentCount: 구간 스플릿 통계를 낼 등간격 X 구간 수(0 = 스플릿 없음). */
data class ChartConfig(val segmentCount: Int = 0, val maxTicks: Int = 5) {
    // ObjC 기본 인자 소실 대응 — 코어 기본값(0, 5) 상속용.
    constructor() : this(0, 5)
}

data class LineChartData(
    val series: List<Series>,
    val referenceBands: List<RefBand> = emptyList(),
    val segmentMarkers: List<Marker> = emptyList(),
    val config: ChartConfig = ChartConfig(),
)
