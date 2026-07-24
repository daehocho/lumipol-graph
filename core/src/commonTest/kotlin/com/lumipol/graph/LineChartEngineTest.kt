package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineChartEngineTest {
    // A(페이스+심박 이중축) + 같은 축 보조 라인 + km 마커/스플릿
    private val data = LineChartData(
        series = listOf(
            Series("pace", listOf(Point(0.0, 6.0), Point(1.0, 5.0), Point(2.0, 5.5)), axis = Axis.PRIMARY, role = SeriesRole.MAIN),
            Series("pace_prev", listOf(Point(0.0, 6.5), Point(1.0, 5.5), Point(2.0, 6.0)), axis = Axis.PRIMARY, role = SeriesRole.MAIN),
            Series("hr", listOf(Point(0.0, 150.0), Point(1.0, 165.0), Point(2.0, 172.0)), axis = Axis.SECONDARY, role = SeriesRole.MAIN),
        ),
        segmentMarkers = listOf(Marker(1.0, label = "1km"), Marker(2.0, label = "2km", emphasis = true)),
        config = ChartConfig(segmentCount = 2),
    )

    @Test
    fun area_only_layout_uses_area_x_range_as_domain() {
        // 시리즈 없이 배경 area만 있는 기록 — X 도메인이 0~1로 붕괴하지 않고 area x범위를 써야 한다.
        // (플랫폼 중립 규칙: 각 렌더러가 도메인을 역산하는 대신 코어가 책임진다.)
        val empty = LineChartData(series = emptyList(), config = ChartConfig(segmentCount = 0, maxTicks = 5))
        val area = listOf(Point(0.0, 10.0), Point(4.0, 40.0), Point(10.0, 20.0))
        val layout = LineChartEngine.layout(empty, backgroundArea = area)
        val xTicks = layout.axisTicks.first { it.axis == ChartAxis.X }.ticks
        assertEquals(0.0, xTicks.first().value, 1e-9)
        assertEquals(10.0, xTicks.last().value, 1e-9)
    }

    @Test
    fun area_only_layout_falls_back_to_plain_layout_when_area_degenerate() {
        val empty = LineChartData(series = emptyList(), config = ChartConfig(segmentCount = 0, maxTicks = 5))
        // 점 1개(도메인 폭 0) → 일반 layout과 동일하게 폴백(크래시 금지).
        val layout = LineChartEngine.layout(empty, backgroundArea = listOf(Point(3.0, 1.0)))
        assertEquals(LineChartEngine.layout(empty), layout)
    }

    @Test
    fun produces_layout_for_every_series() {
        val layout = LineChartEngine.layout(data)
        assertEquals(3, layout.series.size)
        // 정규화 범위 검증
        layout.series.flatMap { it.points }.forEach {
            assertTrue(it.x in 0.0..1.0 && it.y in 0.0..1.0)
        }
        // 역할 보존
        assertEquals(SeriesRole.MAIN, layout.series.first { it.id == "pace_prev" }.role)
    }

    @Test
    fun emits_x_and_both_y_axes_ticks() {
        val layout = LineChartEngine.layout(data)
        val axes = layout.axisTicks.map { it.axis }.toSet()
        assertEquals(setOf(ChartAxis.X, ChartAxis.Y_PRIMARY, ChartAxis.Y_SECONDARY), axes)
    }

    @Test
    fun markers_are_normalized() {
        val layout = LineChartEngine.layout(data)
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
    fun orphan_axis_with_only_ref_band_still_gets_ticks() {
        // SECONDARY 축엔 시리즈가 없고 RefBand만 있어도 axisTicks에 Y_SECONDARY가 나와야 한다.
        val d = LineChartData(
            series = listOf(
                Series("pace", listOf(Point(0.0, 6.0), Point(1.0, 5.0)), axis = Axis.PRIMARY, role = SeriesRole.MAIN),
            ),
            referenceBands = listOf(RefBand(165.0, 175.0, axis = Axis.SECONDARY)),
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

    @Test
    fun overlay_series_is_self_normalized_and_excluded_from_primary_domain() {
        // primary 라인: y 0~100. overlay 라인: y 1000~2000 (다른 스케일).
        val primary = Series(
            id = "p",
            points = listOf(Point(0.0, 0.0), Point(1.0, 100.0)),
            axis = Axis.PRIMARY,
            role = SeriesRole.MAIN,
        )
        val overlay = Series(
            id = "o",
            points = listOf(Point(0.0, 1000.0), Point(0.5, 1500.0), Point(1.0, 2000.0)),
            axis = Axis.PRIMARY,
            role = SeriesRole.OVERLAY,
        )
        val layout = LineChartEngine.layout(
            LineChartData(series = listOf(primary, overlay))
        )

        // overlay는 자체 min(1000)~max(2000)으로 정규화 → 0.0, 0.5, 1.0
        val o = layout.series.first { it.id == "o" }
        assertEquals(SeriesRole.OVERLAY, o.role)
        assertEquals(0.0, o.points[0].y, 1e-6)
        assertEquals(0.5, o.points[1].y, 1e-6)
        assertEquals(1.0, o.points[2].y, 1e-6)

        // primary 축 틱 범위는 overlay(1000~2000)의 영향을 받지 않아야 한다.
        val yPrimaryTicks = layout.axisTicks.first { it.axis == ChartAxis.Y_PRIMARY }.ticks
        assertTrue(yPrimaryTicks.all { it.value <= 200.0 })
    }
}
