package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ViewportLayoutTest {
    // x 0..4 등간격, y는 x=2에서 봉우리(10), 나머지 1
    private val data = LineChartData(
        series = listOf(
            Series(
                "pace",
                listOf(
                    Point(0.0, 1.0), Point(1.0, 1.0), Point(2.0, 10.0),
                    Point(3.0, 1.0), Point(4.0, 1.0),
                ),
            ),
        ),
        referenceLines = listOf(RefLine(5.0, axis = Axis.PRIMARY)),
        segmentMarkers = listOf(Marker(1.0, label = "1km"), Marker(3.5, label = "3.5km")),
    )

    @Test
    fun window_domain_is_exact_and_ticks_stay_inside() {
        val layout = LineChartEngine.layout(data, 1.0, 3.0)
        val xTicks = layout.axisTicks.first { it.axis == ChartAxis.X }.ticks
        xTicks.forEach { assertTrue(it.value in 1.0..3.0) }
        // 구간 양끝 값이 position 0/1에 온다: x=1 tick position 0, x=3 tick position 1
        assertEquals(0.0, xTicks.first { it.value == 1.0 }.position, 1e-9)
        assertEquals(1.0, xTicks.first { it.value == 3.0 }.position, 1e-9)
    }

    @Test
    fun includes_one_neighbor_point_each_side() {
        val layout = LineChartEngine.layout(data, 1.5, 2.5)
        val points = layout.series[0].points
        // 구간 내 포인트는 x=2 하나지만 이웃(x=1, x=3)이 포함돼 3개
        assertEquals(3, points.size)
        // 이웃은 정규화 0..1 밖
        assertTrue(points.first().x < 0.0)
        assertTrue(points.last().x > 1.0)
    }

    @Test
    fun y_domain_refits_to_visible_points_only() {
        // 봉우리(x=2, y=10)가 창 밖 → Y 도메인은 y=1과 기준선 5로 재계산
        // (창 안 y 최대 = 5(기준선) → 정규화에서 y=10 이웃 포인트는 1 초과)
        val layout = LineChartEngine.layout(data, 2.5, 4.0)
        val inWindow = layout.series[0].points.filter { it.x in 0.0..1.0 }
        inWindow.forEach { assertTrue(it.y in 0.0..1.0) }
        // 기준선은 항상 Y 도메인 안 (position 0..1)
        assertTrue(layout.refLines[0].position in 0.0..1.0)
    }

    @Test
    fun markers_outside_window_are_dropped() {
        val layout = LineChartEngine.layout(data, 0.0, 2.0)
        // 1.0 마커만 남고 3.5 마커는 제거
        assertEquals(1, layout.markers.size)
        assertEquals("1km", layout.markers[0].label)
    }

    @Test
    fun window_in_gap_between_points_returns_crossing_neighbors() {
        val sparse = LineChartData(
            series = listOf(Series("s", listOf(Point(0.0, 1.0), Point(10.0, 2.0)))),
        )
        val layout = LineChartEngine.layout(sparse, 4.0, 6.0)
        // 창 안에 포인트가 없어도 가로지르는 이웃 쌍 2개가 남아 선이 그려진다
        assertEquals(2, layout.series[0].points.size)
    }

    @Test
    fun invalid_window_throws() {
        assertFailsWith<IllegalArgumentException> { LineChartEngine.layout(data, 3.0, 3.0) }
    }

    @Test
    fun stats_and_segments_ignore_viewport() {
        val full = LineChartEngine.layout(
            data.copy(config = ChartConfig(segmentCount = 2)))
        val windowed = LineChartEngine.layout(
            data.copy(config = ChartConfig(segmentCount = 2)), 1.5, 2.5)
        assertEquals(full.stats.segments, windowed.stats.segments)
        assertEquals(full.stats.perSeries, windowed.stats.perSeries)
    }
}
