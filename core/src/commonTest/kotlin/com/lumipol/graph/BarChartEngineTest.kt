package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BarChartEngineTest {

    // 300초/1000m = 300s/km 세그먼트 3개 = 3km 정확
    private fun evenSamples(km: Int, secPerKm: Double) =
        List(km) { SplitSample(1000.0, secPerKm) }

    @Test
    fun exact_km_produces_one_bar_per_km_no_partial() {
        val data = BarChartData(evenSamples(3, 300.0), splitDistanceMeters = 1000.0)
        val layout = BarChartEngine.layout(data)
        assertEquals(3, layout.bars.size)
        assertTrue(layout.bars.none { it.isPartial })
        layout.bars.forEach { assertEquals(300.0, it.value, 1e-6) }
    }

    @Test
    fun remainder_becomes_partial_last_bar() {
        // 1km(300s) + 0.5km(150s) → 막대 2개, 마지막 부분. value는 sec/unit 정규화(150/0.5=300).
        val data = BarChartData(
            listOf(SplitSample(1000.0, 300.0), SplitSample(500.0, 150.0)),
            splitDistanceMeters = 1000.0,
        )
        val layout = BarChartEngine.layout(data)
        assertEquals(2, layout.bars.size)
        assertTrue(layout.bars[1].isPartial)
        assertEquals(300.0, layout.bars[1].value, 1e-6)
    }

    @Test
    fun value_is_time_weighted_not_arithmetic_mean() {
        // 한 km 안: 900m를 270s(=300s/km 페이스) + 100m를 20s(=200s/km 페이스).
        // 시간가중 = 290s / (1000/1000) = 290. 산술평균이면 (300+200)/2=250 → 다름.
        val data = BarChartData(
            listOf(SplitSample(900.0, 270.0), SplitSample(100.0, 20.0)),
            splitDistanceMeters = 1000.0,
        )
        val layout = BarChartEngine.layout(data)
        assertEquals(1, layout.bars.size)
        assertEquals(290.0, layout.bars[0].value, 1e-6)
    }

    @Test
    fun color_role_relative_to_average_when_no_target() {
        // 페이스: 250, 300, 350 (평균 300). tol=10 → faster, onTarget, slower.
        val data = BarChartData(
            listOf(SplitSample(1000.0, 250.0), SplitSample(1000.0, 300.0), SplitSample(1000.0, 350.0)),
            splitDistanceMeters = 1000.0,
            toleranceSecPerUnit = 10.0,
        )
        val bars = BarChartEngine.layout(data).bars
        assertEquals(BarColorRole.FASTER, bars[0].colorRole)
        assertEquals(BarColorRole.ON_TARGET, bars[1].colorRole)
        assertEquals(BarColorRole.SLOWER, bars[2].colorRole)
    }

    @Test
    fun color_role_uses_target_when_provided() {
        // 실제 평균은 300이지만 목표를 400으로 주면 모두 목표보다 빠름 → 전부 FASTER.
        val data = BarChartData(
            listOf(SplitSample(1000.0, 250.0), SplitSample(1000.0, 300.0), SplitSample(1000.0, 350.0)),
            splitDistanceMeters = 1000.0,
            targetPaceSecPerUnit = 400.0,
            toleranceSecPerUnit = 10.0,
        )
        val bars = BarChartEngine.layout(data).bars
        assertTrue(bars.all { it.colorRole == BarColorRole.FASTER })
    }

    @Test
    fun mile_split_distance_bins_by_mile() {
        // 1609.344m(480s) + 800m(240s) → 1.5마일. 막대 2개, 마지막 부분.
        val data = BarChartData(
            listOf(SplitSample(1609.344, 480.0), SplitSample(800.0, 240.0)),
            splitDistanceMeters = 1609.344,
        )
        val layout = BarChartEngine.layout(data)
        assertEquals(2, layout.bars.size)
        assertTrue(layout.bars[1].isPartial)
    }

    @Test
    fun normalized_outputs_in_range_and_ref_present() {
        val layout = BarChartEngine.layout(
            BarChartData(evenSamples(3, 300.0).mapIndexed { i, s ->
                SplitSample(1000.0, 300.0 + i * 30) }, splitDistanceMeters = 1000.0)
        )
        layout.bars.forEach { assertTrue(it.heightFraction in 0.0..1.0) }
        layout.yTicks.forEach { assertTrue(it.position in 0.0..1.0) }
        assertTrue((layout.referenceLinePosition ?: -1.0) in 0.0..1.0)
    }

    @Test
    fun invalid_and_empty_samples() {
        assertEquals(emptyList(), BarChartEngine.layout(
            BarChartData(emptyList(), 1000.0)).bars)
        // 유효 세그먼트 0개(모두 0/음수) → 빈 레이아웃
        val allInvalid = BarChartEngine.layout(
            BarChartData(listOf(SplitSample(0.0, 0.0), SplitSample(-1.0, 10.0)), 1000.0))
        assertTrue(allInvalid.bars.isEmpty())
        assertNull(allInvalid.referenceLinePosition)
    }

    @Test
    fun single_sample_spanning_multiple_units_splits_correctly() {
        // 2.5km를 750s에 커버(=300s/km) → 1km,1km 두 개 full + 0.5km partial, 전부 300s/unit.
        val data = BarChartData(listOf(SplitSample(2500.0, 750.0)), splitDistanceMeters = 1000.0)
        val bars = BarChartEngine.layout(data).bars
        assertEquals(3, bars.size)
        assertTrue(!bars[0].isPartial && !bars[1].isPartial)
        assertTrue(bars[2].isPartial)
        bars.forEach { assertEquals(300.0, it.value, 1e-6) }
    }

    @Test
    fun single_partial_bar_only() {
        // 0.3km만 있는 짧은 러닝 → 부분 막대 1개, 정규화 안정(min==max 패딩).
        val layout = BarChartEngine.layout(
            BarChartData(listOf(SplitSample(300.0, 90.0)), 1000.0))
        assertEquals(1, layout.bars.size)
        assertTrue(layout.bars[0].isPartial)
        assertTrue(layout.bars[0].heightFraction in 0.0..1.0)
    }
}
