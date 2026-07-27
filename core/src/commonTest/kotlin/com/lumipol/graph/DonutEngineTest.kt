package com.lumipol.graph

import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutColorRole
import com.lumipol.graph.model.DonutSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DonutEngineTest {

    private fun seg(v: Double, role: DonutColorRole = DonutColorRole.ZONE1) = DonutSegment(v, role)

    @Test
    fun sweep_fractions_sum_to_one() {
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(30.0), seg(30.0, DonutColorRole.ZONE2), seg(40.0, DonutColorRole.ZONE3)))
        )
        val sum = layout.segments.sumOf { it.sweepFraction }
        assertEquals(1.0, sum, 1e-6)
        assertEquals(100.0, layout.total, 1e-6)
    }

    @Test
    fun start_fractions_are_cumulative() {
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(25.0), seg(75.0, DonutColorRole.ZONE2)))
        )
        assertEquals(0.0, layout.segments[0].startFraction, 1e-6)
        assertEquals(0.25, layout.segments[0].sweepFraction, 1e-6)
        assertEquals(0.25, layout.segments[1].startFraction, 1e-6)
        assertEquals(0.75, layout.segments[1].sweepFraction, 1e-6)
    }

    @Test
    fun zero_value_segments_are_dropped() {
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(0.0), seg(50.0, DonutColorRole.ZONE2), seg(0.0, DonutColorRole.ZONE3)))
        )
        assertEquals(1, layout.segments.size)
        assertEquals(DonutColorRole.ZONE2, layout.segments[0].colorRole)
        assertEquals(1.0, layout.segments[0].sweepFraction, 1e-6)
    }

    @Test
    fun empty_and_all_zero_input_returns_empty_layout() {
        val empty = DonutEngine.layout(DonutChartData(emptyList()))
        assertTrue(empty.segments.isEmpty())
        assertEquals(0.0, empty.total, 1e-6)

        val allZero = DonutEngine.layout(DonutChartData(listOf(seg(0.0), seg(0.0, DonutColorRole.ZONE2))))
        assertTrue(allZero.segments.isEmpty())
        assertEquals(0.0, allZero.total, 1e-6)
    }

    @Test
    fun layout_carries_source_index_of_original_segments() {
        // value<=0 필터로 레이아웃 인덱스가 원본과 어긋나도, 각 조각이 원본 인덱스를 직접 들고 있어
        // 렌더러 히트테스트가 필터 규칙을 복제하지 않아도 된다.
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(0.0), seg(30.0, DonutColorRole.ZONE2), seg(70.0, DonutColorRole.ZONE3)))
        )
        assertEquals(listOf(1, 2), layout.segments.map { it.sourceIndex })
    }

    @Test
    fun fractions_are_in_unit_range() {
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(10.0), seg(20.0, DonutColorRole.ZONE2), seg(70.0, DonutColorRole.ZONE3)))
        )
        layout.segments.forEach {
            assertTrue(it.startFraction in 0.0..1.0)
            assertTrue(it.sweepFraction in 0.0..1.0)
        }
    }

    @Test
    fun layoutCarriesSegmentLabel() {
        // value<=0 필터 후에도 label이 올바른 세그먼트에 붙어 있어야 한다.
        val layout = DonutEngine.layout(
            DonutChartData(
                listOf(
                    DonutSegment(0.0, DonutColorRole.ZONE1, "워밍업"),
                    DonutSegment(30.0, DonutColorRole.ZONE2, "저강도"),
                    DonutSegment(70.0, DonutColorRole.ZONE3),
                ),
            ),
        )
        assertEquals("저강도", layout.segments[0].label)
        assertNull(layout.segments[1].label)
    }

    @Test
    fun toggleSelectionTransitions() {
        assertNull(DonutEngine.toggleSelection(current = null, tapped = null))   // 무선택 + 밖 탭
        assertEquals(2, DonutEngine.toggleSelection(current = null, tapped = 2)) // 선택
        assertNull(DonutEngine.toggleSelection(current = 2, tapped = 2))         // 재탭 해제
        assertEquals(3, DonutEngine.toggleSelection(current = 2, tapped = 3))    // 선택 이동
        assertNull(DonutEngine.toggleSelection(current = 2, tapped = null))      // 밖 탭 해제
    }

    // B4 — hitTest(비율 공간). 12시 기준 시계방향, 반경 1 ± band/2 대역.

    /** 25%/25%/50% 도넛 — 12시~3시 = 0번, 3시~6시 = 1번, 6시~12시 = 2번. */
    private fun quadLayout() = DonutEngine.layout(
        DonutChartData(
            listOf(seg(25.0), seg(25.0, DonutColorRole.ZONE2), seg(50.0, DonutColorRole.ZONE3)),
        ),
    )

    @Test
    fun hitTest_maps_angle_to_source_index() {
        val layout = quadLayout()
        val band = 0.2
        // 12시 바로 뒤(첫 조각), 3시 직후(둘째), 9시(셋째)
        assertEquals(0, DonutEngine.hitTest(0.01, -1.0, band, layout))
        assertEquals(1, DonutEngine.hitTest(1.0, 0.01, band, layout))
        assertEquals(2, DonutEngine.hitTest(-1.0, 0.0, band, layout))
    }

    @Test
    fun hitTest_rejects_hole_and_outside() {
        val layout = quadLayout()
        assertNull(DonutEngine.hitTest(0.0, 0.0, 0.2, layout))       // 구멍(중심)
        assertNull(DonutEngine.hitTest(0.0, -1.5, 0.2, layout))      // 링 밖
        assertNull(DonutEngine.hitTest(0.0, -0.5, 0.2, layout))      // 구멍(대역 안쪽)
    }

    @Test
    fun hitTest_wide_band_accepts_taps_beyond_visual_ring() {
        val layout = quadLayout()
        // 대역 0.8이면 반경 0.62·1.38까지 허용 — 얇은 링의 48dp 확장(D7) 경로.
        assertEquals(0, DonutEngine.hitTest(0.0, -0.65, 0.8, layout))
        assertEquals(0, DonutEngine.hitTest(0.0, -1.35, 0.8, layout))
        assertNull(DonutEngine.hitTest(0.0, -0.55, 0.8, layout))
    }

    @Test
    fun hitTest_reports_original_source_index_after_zero_filter() {
        // 0값 조각이 걸러져도 원본 인덱스로 보고(레이아웃 인덱스 아님).
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(0.0), seg(50.0, DonutColorRole.ZONE2), seg(50.0, DonutColorRole.ZONE3))),
        )
        assertEquals(1, DonutEngine.hitTest(0.7, -0.7, 0.2, layout)) // 1시 방향 → 첫 표시 조각 = 원본 1
    }

    @Test
    fun hitTest_empty_layout_returns_null() {
        val empty = DonutEngine.layout(DonutChartData(emptyList()))
        assertNull(DonutEngine.hitTest(0.0, -1.0, 0.2, empty))
    }
}
