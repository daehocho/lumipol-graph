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

    // MARK: chooseTimeBucketSeconds

    @Test
    fun bucket_selection_picks_smallest_candidate_within_max_bars() {
        // 12분(720s): 1분→12막대(>10), 2분→6막대(<=10) 선택 → 120s
        assertEquals(120.0, BarChartEngine.chooseTimeBucketSeconds(720.0), 1e-9)
    }

    @Test
    fun bucket_selection_one_minute_for_short_run() {
        // 5분(300s): 1분→5막대(<=10) → 60s
        assertEquals(60.0, BarChartEngine.chooseTimeBucketSeconds(300.0), 1e-9)
    }

    @Test
    fun bucket_selection_exactly_ten_bars_boundary() {
        // 20분(1200s): 1분→20막대(>10), 2분→10막대(<=10) → 120s
        assertEquals(120.0, BarChartEngine.chooseTimeBucketSeconds(1200.0), 1e-9)
        // 10분(600s): 1분→10막대(정확히 10, <=10) → 60s
        assertEquals(60.0, BarChartEngine.chooseTimeBucketSeconds(600.0), 1e-9)
    }

    @Test
    fun bucket_selection_falls_back_to_ten_minutes_for_very_long_run() {
        // 2시간(7200s): 1→120,2→60,5→24,10→12막대 모두 >10 → 마지막 후보 10분 = 600s
        assertEquals(600.0, BarChartEngine.chooseTimeBucketSeconds(7200.0), 1e-9)
    }

    // MARK: 시간모드 집계

    // 5'00"/km(=300s/km) 균일 러닝 N초를 1초 간격 샘플로.
    private fun evenTimeSamples(totalSeconds: Int, secPerKm: Double): List<SplitSample> {
        val dPerSec = 1000.0 / secPerKm // 거리(m)/초
        return List(totalSeconds) { SplitSample(dPerSec, 1.0) }
    }

    @Test
    fun time_mode_bar_count_matches_bucket_selection() {
        // 720s(12분), bucket=120s(2분) → 6막대
        val data = BarChartData(
            evenTimeSamples(720, 300.0), splitDistanceMeters = 1000.0,
            splitTimeSeconds = BarChartEngine.chooseTimeBucketSeconds(720.0),
            totalDurationSeconds = 720.0, totalDistanceMeters = 720 * (1000.0 / 300.0),
        )
        val layout = BarChartEngine.layout(data)
        assertEquals(6, layout.bars.size)
        assertTrue(layout.bars.none { it.isPartial })
    }

    @Test
    fun time_mode_bar_value_is_bucket_average_pace() {
        val data = BarChartData(
            evenTimeSamples(720, 300.0), splitDistanceMeters = 1000.0,
            splitTimeSeconds = 120.0,
            totalDurationSeconds = 720.0, totalDistanceMeters = 720 * (1000.0 / 300.0),
        )
        BarChartEngine.layout(data).bars.forEach { assertEquals(300.0, it.value, 1.0) }
    }

    @Test
    fun time_mode_trailing_remainder_is_partial() {
        // 780s(13분), bucket=120s → 6 full + 1 partial(60s) = 7막대
        val data = BarChartData(
            evenTimeSamples(780, 300.0), splitDistanceMeters = 1000.0,
            splitTimeSeconds = 120.0,
            totalDurationSeconds = 780.0, totalDistanceMeters = 780 * (1000.0 / 300.0),
        )
        val bars = BarChartEngine.layout(data).bars
        assertEquals(7, bars.size)
        assertTrue(bars.last().isPartial)
        assertTrue(!bars.first().isPartial)
    }

    @Test
    fun time_mode_end_minutes_are_cumulative_and_min_one() {
        // 720s, bucket=120s(2분) → endMinutes = 2,4,6,8,10,12
        val data = BarChartData(
            evenTimeSamples(720, 300.0), splitDistanceMeters = 1000.0,
            splitTimeSeconds = 120.0,
            totalDurationSeconds = 720.0, totalDistanceMeters = 720 * (1000.0 / 300.0),
        )
        val ends = BarChartEngine.layout(data).bars.map { it.endMinutes }
        assertEquals(listOf(2, 4, 6, 8, 10, 12), ends)
    }

    @Test
    fun distance_mode_end_minutes_null() {
        val data = BarChartData(evenSamples(3, 300.0), splitDistanceMeters = 1000.0)
        assertTrue(BarChartEngine.layout(data).bars.all { it.endMinutes == null })
    }

    @Test
    fun time_mode_color_ref_from_run_totals_not_sample_sum() {
        // 앞 절반 250s/km(빠름), 뒤 절반 350s/km(느림). 런 총합 평균은 별도로 300 주입.
        // bucket=60s로 각 1분 막대. 총합기반 ref=300, tol=10 → 빠른 막대 FASTER, 느린 막대 SLOWER.
        val fast = evenTimeSamples(180, 250.0)
        val slow = evenTimeSamples(180, 350.0)
        val totalDist = 180 * (1000.0 / 250.0) + 180 * (1000.0 / 350.0)
        val data = BarChartData(
            fast + slow, splitDistanceMeters = 1000.0,
            splitTimeSeconds = 60.0, toleranceSecPerUnit = 10.0,
            totalDurationSeconds = 360.0, totalDistanceMeters = totalDist,
        )
        val bars = BarChartEngine.layout(data).bars
        // 런 총합 평균 ref = 360/(totalDist/1000) ≈ 291.7. 250<281.7 → FASTER, 350>301.7 → SLOWER.
        assertEquals(BarColorRole.FASTER, bars.first().colorRole)
        assertEquals(BarColorRole.SLOWER, bars.last().colorRole)
    }

    @Test
    fun time_mode_equivalence_with_legacy_synthetic_samples() {
        // 동일성 회귀: 구 방식(가짜 샘플을 거리엔진에 투입) vs 신 방식(시간모드) 막대 값 동일.
        val secPerKm = 300.0
        val totalSeconds = 500 // 비정수 버킷 경계 유발
        val real = evenTimeSamples(totalSeconds, secPerKm)
        val bucket = 120.0
        val unit = 1000.0

        // 구 방식 재현: N초 버킷 사전집계 → 합성 SplitSample.
        val legacy = mutableListOf<SplitSample>()
        var accT = 0.0; var accD = 0.0
        fun flush(partial: Boolean) {
            if (accT <= 0.0 || accD <= 0.0) { accT = 0.0; accD = 0.0; return }
            val avg = accT / (accD / unit)
            if (partial) { val f = minOf(1.0, accT / bucket); legacy.add(SplitSample(unit * f, avg * f)) }
            else legacy.add(SplitSample(unit, avg))
            accT = 0.0; accD = 0.0
        }
        for (s in real) { accT += s.timeSeconds; accD += s.distanceMeters; if (accT >= bucket) flush(false) }
        flush(true)
        val legacyBars = BarChartEngine.layout(
            BarChartData(legacy, splitDistanceMeters = unit)).bars

        val newBars = BarChartEngine.layout(
            BarChartData(real, splitDistanceMeters = unit, splitTimeSeconds = bucket)).bars

        assertEquals(legacyBars.size, newBars.size)
        for (i in legacyBars.indices) {
            assertEquals(legacyBars[i].value, newBars[i].value, 1e-6)
            assertEquals(legacyBars[i].isPartial, newBars[i].isPartial)
        }
    }
}
