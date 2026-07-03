package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineChartEngineTest {
    // A(페이스+심박 이중축) + C(고스트 + 목표선) + km 마커/스플릿
    private val data = LineChartData(
        series = listOf(
            Series("pace", listOf(Point(0.0, 6.0), Point(1.0, 5.0), Point(2.0, 5.5)), axis = Axis.PRIMARY, role = SeriesRole.MAIN),
            Series("pace_prev", listOf(Point(0.0, 6.5), Point(1.0, 5.5), Point(2.0, 6.0)), axis = Axis.PRIMARY, role = SeriesRole.GHOST),
            Series("hr", listOf(Point(0.0, 150.0), Point(1.0, 165.0), Point(2.0, 172.0)), axis = Axis.SECONDARY, role = SeriesRole.MAIN),
        ),
        referenceLines = listOf(RefLine(5.3, axis = Axis.PRIMARY, label = "목표 5'18\"")),
        segmentMarkers = listOf(Marker(1.0, label = "1km"), Marker(2.0, label = "2km", emphasis = true)),
        config = ChartConfig(segmentCount = 2),
    )

    @Test
    fun produces_layout_for_every_series() {
        val layout = LineChartEngine.layout(data)
        assertEquals(3, layout.series.size)
        // 정규화 범위 검증
        layout.series.flatMap { it.points }.forEach {
            assertTrue(it.x in 0.0..1.0 && it.y in 0.0..1.0)
        }
        // 역할 보존
        assertEquals(SeriesRole.GHOST, layout.series.first { it.id == "pace_prev" }.role)
    }

    @Test
    fun emits_x_and_both_y_axes_ticks() {
        val layout = LineChartEngine.layout(data)
        val axes = layout.axisTicks.map { it.axis }.toSet()
        assertEquals(setOf(ChartAxis.X, ChartAxis.Y_PRIMARY, ChartAxis.Y_SECONDARY), axes)
    }

    @Test
    fun reference_line_and_markers_are_normalized() {
        val layout = LineChartEngine.layout(data)
        assertEquals(1, layout.refLines.size)
        assertEquals("목표 5'18\"", layout.refLines[0].label)
        assertTrue(layout.refLines[0].position in 0.0..1.0)
        assertEquals(2, layout.markers.size)
        assertTrue(layout.markers[1].emphasis)
        assertTrue(layout.markers[0].position in 0.0..1.0)
    }

    @Test
    fun stats_include_per_series_and_segments() {
        val layout = LineChartEngine.layout(data)
        assertEquals(3, layout.stats.perSeries.size)
        assertEquals(2, layout.stats.segments.size) // MAIN/PRIMARY 시리즈(pace) 기준
        assertEquals("pace", layout.stats.segmentSeriesId)
        layout.stats.segments.forEach { assertTrue(it.count > 0) }
    }

    @Test
    fun nearest_delegates_to_query() {
        val r = LineChartEngine.nearest(data, 0.95)
        assertEquals(3, r.size)
        assertEquals(1.0, r[0].x, 1e-9)
    }

    @Test
    fun orphan_axis_with_only_ref_line_still_gets_ticks() {
        // SECONDARY 축엔 시리즈가 없고 RefLine만 있어도 axisTicks에 Y_SECONDARY가 나와야 한다.
        val d = LineChartData(
            series = listOf(
                Series("pace", listOf(Point(0.0, 6.0), Point(1.0, 5.0)), axis = Axis.PRIMARY, role = SeriesRole.MAIN),
            ),
            referenceLines = listOf(RefLine(170.0, axis = Axis.SECONDARY)),
        )
        val layout = LineChartEngine.layout(d)
        val axes = layout.axisTicks.map { it.axis }.toSet()
        assertTrue(ChartAxis.Y_SECONDARY in axes)
    }

    @Test
    fun x_domain_ends_at_data_max_not_nice_bound() {
        // 데이터 max 10.06 → nice 올림(15) 대신 데이터 끝이 도메인 끝. 15 tick은 생기지 않는다.
        val d = LineChartData(
            series = listOf(Series("pace", (0..100).map { Point(it * 0.1006, 6.0 + it % 3) })),
        )
        val layout = LineChartEngine.layout(d)
        val xTicks = layout.axisTicks.first { it.axis == ChartAxis.X }.ticks
        assertEquals(10.0, xTicks.last().value, 1e-9)
        assertTrue(xTicks.last().position < 1.0)
        assertEquals(1.0, layout.series[0].points.last().x, 1e-9)
    }

    @Test
    fun x_tick_on_data_max_survives_clamp() {
        // 데이터 max가 정확히 tick 값(2.0)인 경우 부동소수 오차로 잘려나가면 안 된다.
        val layout = LineChartEngine.layout(data)
        val xTicks = layout.axisTicks.first { it.axis == ChartAxis.X }.ticks
        assertEquals(2.0, xTicks.last().value, 1e-9)
        assertEquals(1.0, xTicks.last().position, 1e-9)
    }

    @Test
    fun segment_series_id_is_null_when_no_split_requested() {
        val layout = LineChartEngine.layout(data.copy(config = ChartConfig(segmentCount = 0)))
        assertTrue(layout.stats.segments.isEmpty())
        assertEquals(null, layout.stats.segmentSeriesId)
    }
}
