package com.lumipol.graph

import com.lumipol.graph.scale.niceScale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NiceScaleTest {
    @Test
    fun rounds_range_to_nice_bounds_and_ticks() {
        val s = niceScale(0.0, 97.0, maxTicks = 5)
        assertEquals(0.0, s.niceMin, 1e-9)
        assertEquals(100.0, s.niceMax, 1e-9)
        assertEquals(20.0, s.step, 1e-9)
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), s.ticks)
    }

    @Test
    fun handles_flat_data_without_crashing() {
        val s = niceScale(5.0, 5.0)
        assertTrue(s.niceMin < 5.0 && s.niceMax > 5.0)
        assertTrue(s.ticks.isNotEmpty())
    }

    @Test
    fun handles_ulp_wide_span_without_hanging() {
        // 스팬이 1 ULP면 step이 ULP보다 작아 `t += step`이 제자리 → 틱 루프가 안 끝나던 회귀.
        // (막대가 전부 같은 페이스이고 ref만 1 ULP 다른 런에서 실측)
        val lo = 300.00000000000034
        val hi = 300.0000000000004
        assertTrue(hi - lo > 0.0 && lo + (hi - lo) / 4.0 == lo, "테스트 전제: 스팬이 ULP 수준")
        val s = niceScale(lo, hi, maxTicks = 5, headroomFraction = 0.05)
        assertTrue(s.ticks.size in 2..8, "틱이 유한 개여야 한다: ${s.ticks.size}")
        assertTrue(s.niceMin < lo && s.niceMax > hi)
    }

    @Test
    fun handles_reversed_min_max() {
        val s = niceScale(97.0, 0.0)
        assertEquals(0.0, s.niceMin, 1e-9)
        assertEquals(100.0, s.niceMax, 1e-9)
    }

    @Test
    fun works_with_small_max_ticks() {
        val s = niceScale(0.0, 10.0, maxTicks = 3)
        assertTrue(s.niceMin <= 0.0 && s.niceMax >= 10.0)
        assertTrue(s.ticks.isNotEmpty())
    }

    @Test
    fun rejects_max_ticks_below_two() {
        assertFailsWith<IllegalArgumentException> { niceScale(0.0, 10.0, maxTicks = 1) }
    }

    // ---- headroomFraction (Y축 헤드룸) ----

    @Test
    fun headroom_default_zero_keeps_boundary_hugging_domain() {
        // 회귀 가드 겸 현행 문제 문서화: HR 100~180이 step 20 배수에 걸려 축이 딱 붙는다.
        val s = niceScale(100.0, 180.0, maxTicks = 5)
        assertEquals(100.0, s.niceMin, 1e-9)
        assertEquals(180.0, s.niceMax, 1e-9)
    }

    @Test
    fun headroom_expands_boundary_hugging_domain_both_ends() {
        // 100~180 → 5% 인플레이션 96~184 → step 20 → 80~200
        val s = niceScale(100.0, 180.0, maxTicks = 5, headroomFraction = 0.05)
        assertEquals(80.0, s.niceMin, 1e-9)
        assertEquals(200.0, s.niceMax, 1e-9)
        assertEquals(listOf(80.0, 100.0, 120.0, 140.0, 160.0, 180.0, 200.0), s.ticks)
    }

    @Test
    fun headroom_keeps_bound_that_already_has_room() {
        // 100~175 → 인플레이션 96.25~178.75 → step 20 → 80~180 (max쪽은 이미 여유, 그대로)
        val s = niceScale(100.0, 175.0, maxTicks = 5, headroomFraction = 0.05)
        assertEquals(80.0, s.niceMin, 1e-9)
        assertEquals(180.0, s.niceMax, 1e-9)
    }

    @Test
    fun headroom_never_pushes_nonnegative_min_below_zero() {
        // 0~180 → min은 0 클램프, max만 확장 (음수 불가 지표 보호)
        val s = niceScale(0.0, 180.0, maxTicks = 5, headroomFraction = 0.05)
        assertEquals(0.0, s.niceMin, 1e-9)
        assertEquals(200.0, s.niceMax, 1e-9)
    }

    @Test
    fun headroom_pads_negative_min_without_clamp() {
        // 고도처럼 원래 음수인 데이터는 클램프 없이 아래로도 패딩
        val s = niceScale(-20.0, 180.0, maxTicks = 5, headroomFraction = 0.05)
        assertTrue(s.niceMin < -20.0)
        assertTrue(s.niceMax > 180.0)
    }

    @Test
    fun headroom_works_on_flat_data() {
        // min == max 축퇴 재귀에 headroomFraction이 전달되어도 예외 없이 동작
        val s = niceScale(150.0, 150.0, headroomFraction = 0.05)
        assertTrue(s.niceMin < 150.0 && s.niceMax > 150.0)
        assertTrue(s.ticks.isNotEmpty())
    }
}
