package com.lumipol.graph.renderer

import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.ChartConfig
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.Marker
import com.lumipol.graph.model.Point
import com.lumipol.graph.model.RefBand
import com.lumipol.graph.model.Series
import com.lumipol.graph.model.SeriesRole
import kotlin.math.roundToInt

// iOS: TestFixtures.swift — 스냅샷·터치 테스트 공용 데이터(x = km, 0.0~5.0, 0.5 간격 11점).
internal object TestFixtures {
    val paceValues = listOf(6.1, 5.9, 5.75, 5.6, 5.7, 5.5, 5.35, 5.45, 5.3, 5.2, 5.4)
    val heartRateValues = listOf(148.0, 152.0, 157.0, 160.0, 163.0, 166.0, 168.0, 170.0, 172.0, 174.0, 171.0)
    val ghostPaceValues = listOf(6.4, 6.2, 6.15, 6.0, 6.05, 5.9, 5.8, 5.85, 5.7, 5.65, 5.75)

    fun series(id: String, values: List<Double>, axis: Axis, role: SeriesRole): Series {
        val points = values.mapIndexed { i, v -> Point(x = i * 0.5, y = v) }
        return Series(id = id, points = points, axis = axis, role = role)
    }

    val kmMarkers: List<Marker>
        get() = (1..5).map { Marker(x = it.toDouble(), label = "${it}km", emphasis = it == 5) }

    val fullChart: LineChartData
        get() = LineChartData(
            series = listOf(
                series("pace", paceValues, Axis.PRIMARY, SeriesRole.MAIN),
                series("pace_prev", ghostPaceValues, Axis.PRIMARY, SeriesRole.GHOST),
                series("hr", heartRateValues, Axis.SECONDARY, SeriesRole.MAIN),
            ),
            referenceBands = listOf(RefBand(lower = 5.4, upper = 5.6, axis = Axis.PRIMARY)),
            segmentMarkers = kmMarkers,
            config = ChartConfig(segmentCount = 5, maxTicks = 5),
        )

    /** ① 페이스 단선(iOS `paceOnly`). */
    val paceOnly: LineChartData
        get() = LineChartData(
            series = listOf(series("pace", paceValues, Axis.PRIMARY, SeriesRole.MAIN)),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )

    /** ② 페이스+심박 이중축 + km 마커(iOS `paceAndHeartRate`). */
    val paceAndHeartRate: LineChartData
        get() = LineChartData(
            series = listOf(
                series("pace", paceValues, Axis.PRIMARY, SeriesRole.MAIN),
                series("hr", heartRateValues, Axis.SECONDARY, SeriesRole.MAIN),
            ),
            segmentMarkers = kmMarkers,
            config = ChartConfig(segmentCount = 5, maxTicks = 5),
        )

    /** ⑤ 시리즈 없음 — 배경 area(고도)만 있는 기록(iOS `emptySeries`). */
    val emptySeries: LineChartData
        get() = LineChartData(series = emptyList(), config = ChartConfig(segmentCount = 0, maxTicks = 5))

    /** 배경 area 픽셀 좌표 대신 core `Point`로 표현(로컬 AreaPoint 폐기 — arch 계약). */
    fun area(vararg xy: Pair<Double, Double>): List<Point> = xy.map { Point(it.first, it.second) }

    /** 페이스 mm'ss", 심박 정수, X는 km — 앱 포매터 주입 규약의 테스트 구현. */
    fun format(axis: ChartAxis, value: Double): String = when (axis) {
        ChartAxis.Y_PRIMARY -> {
            val minutes = value.toInt()
            val seconds = ((value - minutes) * 60).roundToInt()
            "%d'%02d\"".format(minutes, seconds)
        }
        ChartAxis.Y_SECONDARY -> value.toInt().toString()
        else -> "%gkm".format(value)
    }
}
