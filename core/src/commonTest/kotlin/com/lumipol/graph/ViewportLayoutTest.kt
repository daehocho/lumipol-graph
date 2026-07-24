package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ViewportLayoutTest {
    // x 0..4 등간격, y는 x=2에서 봉우리(10), 나머지는 1~2.
    // x=4의 y=2는 의도적 — 창(2.5~4.0) 안에 서로 다른 y가 둘(1, 2) 있어야
    // y_domain_refits_to_visible_points_only가 실제 재계산을 검증한다(전부 같은 값이면
    // niceScale의 min==max 패딩 분기를 타 모든 점이 0.5로 뭉개져 단언이 무의미해진다).
    private val data = LineChartData(
        series = listOf(
            Series(
                "pace",
                listOf(
                    Point(0.0, 1.0), Point(1.0, 1.0), Point(2.0, 10.0),
                    Point(3.0, 1.0), Point(4.0, 2.0),
                ),
            ),
        ),
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
        // 봉우리(x=2, y=10)가 창 밖 → Y 도메인은 창 안 포인트(y=1, y=2)만으로 재계산된다.
        val layout = LineChartEngine.layout(data, 2.5, 4.0)
        val inWindow = layout.series[0].points.filter { it.x in 0.0..1.0 }
        assertEquals(2, inWindow.size)
        inWindow.forEach { assertTrue(it.y in 0.0..1.0) }
        // 창 안 값이 0..1을 꽉 채운다 = 도메인이 딱 가시 범위(1~2)로 맞춰졌다는 뜻.
        // (봉우리 10이 도메인에 남아 있으면 두 점 모두 0 근처로 눌려 이 단언이 깨진다.)
        assertEquals(0.0, inWindow.minOf { it.y }, 1e-9)
        assertEquals(1.0, inWindow.maxOf { it.y }, 1e-9)
        // 창 밖 이웃(봉우리)은 도메인에 기여하지 않으므로 1을 크게 넘긴다.
        val peakNeighbor = layout.series[0].points.first { it.x < 0.0 }
        assertTrue(peakNeighbor.y > 1.0, "창 밖 봉우리 이웃 y=${peakNeighbor.y}는 1 초과여야 한다")
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
