package com.lumipol.graph

import com.lumipol.graph.model.*
import com.lumipol.graph.query.SCRUB_WINDOW_EPSILON
import com.lumipol.graph.query.nearestScrub
import com.lumipol.graph.scale.AxisDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** B2 — 스크럽 결과(창 필터·스냅 소스·정규화 좌표)의 코어 확정 규칙. 구 렌더러 TouchMarker 동작 불변. */
class ScrubTest {

    private val data = LineChartData(
        series = listOf(
            Series("pace", listOf(Point(0.0, 5.0), Point(1.0, 6.0), Point(2.0, 4.0))),
            Series("hr", listOf(Point(0.0, 150.0), Point(1.0, 160.0), Point(2.0, 170.0)), axis = Axis.SECONDARY),
            Series("alt", listOf(Point(0.0, 10.0), Point(2.0, 30.0)), role = SeriesRole.OVERLAY),
        ),
    )
    private val layout = LineChartEngine.layout(data)

    @Test
    fun snap_source_prefers_main_series() {
        // 첫 시리즈가 오버레이여도 스냅 소스는 main.
        val d = LineChartData(
            series = listOf(
                Series("alt", listOf(Point(0.0, 10.0), Point(2.0, 30.0)), role = SeriesRole.OVERLAY),
                Series("pace", listOf(Point(0.0, 5.0), Point(1.0, 6.0))),
            ),
        )
        val r = nearestScrub(d, LineChartEngine.layout(d), 0.9)
        assertNotNull(r)
        assertEquals("pace", r.snapSourceId)
        assertEquals(1.0, r.snappedX)
    }

    @Test
    fun snap_source_falls_back_to_first_series_without_main() {
        val d = LineChartData(
            series = listOf(
                Series("alt", listOf(Point(0.0, 10.0), Point(2.0, 30.0)), role = SeriesRole.OVERLAY),
                Series("cad", listOf(Point(0.0, 80.0), Point(2.0, 90.0)), role = SeriesRole.OVERLAY),
            ),
        )
        val r = nearestScrub(d, LineChartEngine.layout(d), 1.9)
        assertNotNull(r)
        assertEquals("alt", r.snapSourceId)
    }

    @Test
    fun per_series_carries_original_values_and_normalized_coordinates() {
        val r = nearestScrub(data, layout, 0.9)
        assertNotNull(r)
        val pace = r.perSeries.first { it.seriesId == "pace" }
        // 원본 복사(T1) — 근접점 값 그대로.
        assertEquals(1.0, pace.x)
        assertEquals(6.0, pace.y)
        assertEquals(ChartAxis.Y_PRIMARY, pace.chartAxis)
        val yDom = layout.domains.yPrimary!!
        assertEquals(yDom.normalize(6.0), pace.ny)
        assertEquals(layout.domains.x.normalize(1.0), pace.nx)

        val hr = r.perSeries.first { it.seriesId == "hr" }
        assertEquals(ChartAxis.Y_SECONDARY, hr.chartAxis)
        assertEquals(layout.domains.ySecondary!!.normalize(160.0), hr.ny)
        // 모든 도트는 스냅 수직선 위 — nx는 시리즈와 무관하게 동일.
        assertEquals(pace.nx, hr.nx)
        assertEquals(r.snappedNx, pace.nx)
    }

    @Test
    fun overlay_ny_comes_from_layout_self_normalized_points() {
        val r = nearestScrub(data, layout, 1.9)
        assertNotNull(r)
        val alt = r.perSeries.first { it.seriesId == "alt" }
        assertEquals(SeriesRole.OVERLAY, alt.role)
        assertEquals(ChartAxis.Y_OVERLAY, alt.chartAxis)
        // layout의 오버레이 포인트(자체 정규화) 중 근접점 y == 30 → 자체 도메인 10..30에서 1.0.
        assertEquals(1.0, alt.ny)
        assertEquals(30.0, alt.y)
    }

    @Test
    fun overlay_missing_from_layout_reports_value_without_dot() {
        // layout에 오버레이 시리즈가 없으면 ny=null(값만 전달).
        val bare = LineChartLayout(
            series = emptyList(),
            axisTicks = emptyList(),
            refBands = emptyList(),
            markers = emptyList(),
            stats = Stats(emptyList(), emptyList(), null),
            domains = layout.domains,
        )
        val r = nearestScrub(data, bare, 0.9)
        assertNotNull(r)
        val alt = r.perSeries.first { it.seriesId == "alt" }
        assertNull(alt.ny)
        assertEquals(10.0, alt.y)
    }

    @Test
    fun duplicate_series_ids_resolve_axis_per_item_not_first_wins() {
        // 코어는 id 유일성을 강제하지 않는다 — 중복 id가 서로 다른 축이면 각 항목이
        // 자기 축 도메인으로 정규화돼야 한다(그리기 B10 per-item axis와 동일 해석).
        val d = LineChartData(
            series = listOf(
                Series("dup", listOf(Point(0.0, 5.0), Point(1.0, 6.0))),
                Series("dup", listOf(Point(0.0, 150.0), Point(1.0, 160.0)), axis = Axis.SECONDARY),
            ),
        )
        val l = LineChartEngine.layout(d)
        val r = nearestScrub(d, l, 0.9)
        assertNotNull(r)
        assertEquals(2, r.perSeries.size)
        val (first, second) = r.perSeries
        assertEquals(ChartAxis.Y_PRIMARY, first.chartAxis)
        assertEquals(l.domains.yPrimary!!.normalize(6.0), first.ny)
        assertEquals(ChartAxis.Y_SECONDARY, second.chartAxis)
        assertEquals(l.domains.ySecondary!!.normalize(160.0), second.ny)
    }

    @Test
    fun windowed_layout_excludes_series_with_no_points_inside_window() {
        // 창 [0.5, 1.5]: pace/hr는 창 안 1.0 포인트 보유, short는 전 포인트(2.0)가 창 밖 → 결과 제외.
        val d = LineChartData(
            series = data.series + Series("short", listOf(Point(2.0, 99.0))),
        )
        val w = LineChartEngine.layout(d, 0.5, 1.5)
        val r = nearestScrub(d, w, 1.4)
        assertNotNull(r)
        assertTrue(r.perSeries.none { it.seriesId == "short" })
        assertEquals("pace", r.snapSourceId)
    }

    @Test
    fun degenerate_domain_returns_null() {
        val degenerate = LineChartLayout(
            series = emptyList(),
            axisTicks = emptyList(),
            refBands = emptyList(),
            markers = emptyList(),
            stats = Stats(emptyList(), emptyList(), null),
            domains = ChartDomains(AxisDomain(5.0, 5.0), null, null),
        )
        assertNull(nearestScrub(data, degenerate, 5.0))
    }

    @Test
    fun axis_series_without_domain_is_excluded_and_alone_yields_null() {
        // SECONDARY 시리즈인데 ySecondary 도메인이 없는 layout — 도트·값 모두 불능 → 결과 null.
        val d = LineChartData(series = listOf(Series("hr", listOf(Point(0.0, 150.0)), axis = Axis.SECONDARY)))
        val noSecondary = LineChartLayout(
            series = emptyList(),
            axisTicks = emptyList(),
            refBands = emptyList(),
            markers = emptyList(),
            stats = Stats(emptyList(), emptyList(), null),
            domains = ChartDomains(AxisDomain(0.0, 1.0), AxisDomain(0.0, 1.0), null),
        )
        assertNull(nearestScrub(d, noSecondary, 0.0))
    }

    @Test
    fun domain_edge_within_epsilon_clamps_instead_of_dropping() {
        // 질의가 도메인 오른쪽 끝 — 마지막 포인트로 스냅되고 nx는 1.0으로 클램프.
        val r = nearestScrub(data, layout, layout.domains.x.max)
        assertNotNull(r)
        assertEquals(2.0, r.snappedX)
        assertTrue(r.snappedNx in 0.0..1.0)
    }

    @Test
    fun epsilon_constant_matches_renderer_measured_value() {
        assertEquals(1e-9, SCRUB_WINDOW_EPSILON)
    }
}
