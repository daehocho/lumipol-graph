package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        // 역할 보존은 overlay_series_is_self_normalized... 가 MAIN과 구분되는 역할로 검증한다
        // (여기서 MAIN을 단언해봐야 Series.role 기본값과 같아 통과가 보장된다).
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
    @Suppress("DEPRECATION") // deprecated 위임 경로도 제거 시점까지 동작을 보증한다
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

    @Test
    fun y_domain_gets_headroom_on_both_ends() {
        // HR 100~180: 5% 인플레이션 96~184 → step 20 → 축 80~200.
        // 페이스는 렌더러에서 축 반전(위=빠름)이라 min쪽 확장이 화면 위 여유가 된다.
        val d = LineChartData(
            series = listOf(
                Series("hr", listOf(Point(0.0, 100.0), Point(1.0, 180.0)), axis = Axis.SECONDARY),
            ),
        )
        val ticks = LineChartEngine.layout(d).axisTicks
            .first { it.axis == ChartAxis.Y_SECONDARY }.ticks
        assertEquals(80.0, ticks.first().value, 1e-9)
        assertEquals(200.0, ticks.last().value, 1e-9)
    }

    @Test
    fun secondary_axis_merges_heart_and_cadence_domain() {
        // 0.29.0 슬롯 규약의 전제: 심박(110~177)+케이던스(60~180)가 보조축 하나를 공유하면
        // 병합 min/max 60~180에 5% 헤드룸(54~186) → step 50 → 축 50~200 하나가 나오고,
        // 두 시리즈 모두 그 도메인으로 정규화된다(축 눈금으로 읽힘).
        val d = LineChartData(
            series = listOf(
                Series("hr", listOf(Point(0.0, 110.0), Point(1.0, 177.0)), axis = Axis.SECONDARY),
                Series("cad", listOf(Point(0.0, 60.0), Point(1.0, 180.0)), axis = Axis.SECONDARY),
            ),
        )
        val layout = LineChartEngine.layout(d)
        val secondaryTicks = layout.axisTicks.filter { it.axis == ChartAxis.Y_SECONDARY }
        assertEquals(1, secondaryTicks.size)
        val ticks = secondaryTicks.single().ticks
        assertEquals(50.0, ticks.first().value, 1e-9)
        assertEquals(200.0, ticks.last().value, 1e-9)
        // 정규화 y = (값 - 50) / 150 — 두 시리즈가 같은 선형 관계를 쓴다.
        val hr = layout.series.first { it.id == "hr" }.points
        val cad = layout.series.first { it.id == "cad" }.points
        assertEquals((110.0 - 50.0) / 150.0, hr[0].y, 1e-9)
        assertEquals((177.0 - 50.0) / 150.0, hr[1].y, 1e-9)
        assertEquals((60.0 - 50.0) / 150.0, cad[0].y, 1e-9)
        assertEquals((180.0 - 50.0) / 150.0, cad[1].y, 1e-9)
    }

    // MARK: Y_OVERLAY 고도 눈금 (0.40.0)

    private val overlayArea = listOf(Point(0.0, 10.0), Point(2.0, 14.0), Point(4.0, 12.0), Point(6.0, 20.0))

    private fun overlayTicks(layout: LineChartLayout) =
        layout.axisTicks.firstOrNull { it.axis == ChartAxis.Y_OVERLAY }?.ticks

    @Test
    fun overlay_ticks_emitted_when_secondary_axis_free() {
        // 페이스(PRIMARY)+고도 — SECONDARY 비어 있음 → min/max 2눈금, position은 밴드 내 fraction
        val d = LineChartData(series = listOf(Series("pace", listOf(Point(0.0, 5.0), Point(6.0, 6.0)))))
        val ticks = overlayTicks(LineChartEngine.layout(d, overlayArea))
        assertNotNull(ticks)
        assertEquals(listOf(10.0 to 0.0, 20.0 to 1.0), ticks.map { it.value to it.position })
    }

    @Test
    fun overlay_ticks_suppressed_when_secondary_occupied() {
        // 페이스+심박(SECONDARY)+고도 — 양축 점유 → 실루엣만(눈금 미방출)
        val d = LineChartData(series = listOf(
            Series("pace", listOf(Point(0.0, 5.0), Point(6.0, 6.0))),
            Series("hr", listOf(Point(0.0, 120.0), Point(6.0, 160.0)), axis = Axis.SECONDARY),
        ))
        assertNull(overlayTicks(LineChartEngine.layout(d, overlayArea)))
    }

    @Test
    fun overlay_ticks_emitted_for_area_only_layout() {
        // 고도 단독 — 시리즈 없음 → 방출(기존 BG01 경로)
        val ticks = overlayTicks(LineChartEngine.layout(LineChartData(emptyList()), overlayArea))
        assertNotNull(ticks)
        assertEquals(listOf(10.0, 20.0), ticks.map { it.value })
    }

    @Test
    fun overlay_ticks_absent_without_area() {
        val d = LineChartData(series = listOf(Series("pace", listOf(Point(0.0, 5.0), Point(6.0, 6.0)))))
        assertNull(overlayTicks(LineChartEngine.layout(d)))
        assertNull(overlayTicks(LineChartEngine.layout(d, null)))
        assertNull(overlayTicks(LineChartEngine.layout(d, listOf(Point(1.0, 5.0))))) // 2점 미만 퇴화
    }

    @Test
    fun overlay_ticks_flat_area_emits_single_tick() {
        // 완전 평지 — min==max → 겹치는 라벨 대신 1눈금
        val flat = listOf(Point(0.0, 10.0), Point(6.0, 10.0))
        val ticks = overlayTicks(LineChartEngine.layout(LineChartData(emptyList()), flat))
        assertNotNull(ticks)
        assertEquals(listOf(10.0 to 0.0), ticks.map { it.value to it.position })
    }

    @Test
    fun overlay_ticks_near_flat_uses_min_span_floor() {
        // 고저차 0.2m < AREA_MIN_VALUE_SPAN 0.5 → 실루엣과 동일하게 분모 하한 적용, max 위치 0.4
        val nearFlat = listOf(Point(0.0, 10.0), Point(6.0, 10.2))
        val ticks = overlayTicks(LineChartEngine.layout(LineChartData(emptyList()), nearFlat))
        assertNotNull(ticks)
        assertEquals(2, ticks.size)
        assertEquals(10.2, ticks[1].value, 1e-9)
        assertEquals(0.2 / ChartDefaults.AREA_MIN_VALUE_SPAN, ticks[1].position, 1e-9)
    }

    @Test
    fun overlay_ticks_in_windowed_layout_match_full_basis() {
        // 줌 창 layout(4-인자 오버로드) — 실루엣이 전체 정규화를 유지하므로 눈금도 전체 기준
        val d = LineChartData(series = listOf(Series("pace", listOf(Point(0.0, 5.0), Point(6.0, 6.0)))))
        val full = overlayTicks(LineChartEngine.layout(d, overlayArea))
        val windowed = overlayTicks(LineChartEngine.layout(d, 1.0, 3.0, overlayArea))
        assertEquals(full, windowed)
    }
}
