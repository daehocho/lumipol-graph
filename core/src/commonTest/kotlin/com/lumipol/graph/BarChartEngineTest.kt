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
    fun y_domain_gets_headroom_on_both_ends() {
        // 페이스 250/300/350(ref=평균 300): 5% 인플레이션 245~355 → step 50 → 축 200~400
        val data = BarChartData(
            listOf(SplitSample(1000.0, 250.0), SplitSample(1000.0, 300.0), SplitSample(1000.0, 350.0)),
            splitDistanceMeters = 1000.0,
        )
        val ticks = BarChartEngine.layout(data).yTicks
        assertEquals(200.0, ticks.first().value, 1e-9)
        assertEquals(400.0, ticks.last().value, 1e-9)
    }

    @Test
    fun exact_km_produces_one_bar_per_km_no_partial() {
        // 5km 정확(막대 5개 → 버킷 하향 없음) → 1km당 1막대, 부분 없음
        val data = BarChartData(evenSamples(5, 300.0), splitDistanceMeters = 1000.0)
        val layout = BarChartEngine.layout(data)
        assertEquals(5, layout.bars.size)
        assertTrue(layout.bars.none { it.isPartial })
        layout.bars.forEach { assertEquals(300.0, it.value, 1e-6) }
    }

    @Test
    fun remainder_becomes_partial_last_bar() {
        // 4km(1200s) + 0.5km(150s) = 4.5km → 5막대(버킷 1km 유지), 마지막 부분.
        // value는 sec/unit 정규화(150/0.5=300).
        val data = BarChartData(
            evenSamples(4, 300.0) + SplitSample(500.0, 150.0),
            splitDistanceMeters = 1000.0,
        )
        val layout = BarChartEngine.layout(data)
        assertEquals(5, layout.bars.size)
        assertTrue(layout.bars[4].isPartial)
        assertEquals(300.0, layout.bars[4].value, 1e-6)
    }

    @Test
    fun value_is_time_weighted_not_arithmetic_mean() {
        // 마지막 km 안: 900m를 270s(=300s/km 페이스) + 100m를 20s(=200s/km 페이스).
        // 시간가중 = 290s / (1000/1000) = 290. 산술평균이면 (300+200)/2=250 → 다름.
        // 앞 4km는 버킷을 1km로 유지시키기 위한 채움(막대 5개).
        val data = BarChartData(
            evenSamples(4, 300.0) + listOf(SplitSample(900.0, 270.0), SplitSample(100.0, 20.0)),
            splitDistanceMeters = 1000.0,
        )
        val layout = BarChartEngine.layout(data)
        assertEquals(5, layout.bars.size)
        assertEquals(290.0, layout.bars[4].value, 1e-6)
    }

    @Test
    fun color_role_relative_to_average_when_no_target() {
        // 페이스: 250, 300, 350, 300, 300 (시간가중 평균 300). tol=10 → faster, onTarget, slower.
        val data = BarChartData(
            listOf(250.0, 300.0, 350.0, 300.0, 300.0).map { SplitSample(1000.0, it) },
            splitDistanceMeters = 1000.0,
            toleranceSecPerUnit = 10.0,
        )
        val bars = BarChartEngine.layout(data).bars
        assertEquals(5, bars.size)
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
        // 4마일(1920s) + 800m(240s) = 4.5마일 → 5막대(버킷 마일 유지), 마지막 부분.
        val data = BarChartData(
            List(4) { SplitSample(1609.344, 480.0) } + SplitSample(800.0, 240.0),
            splitDistanceMeters = 1609.344,
        )
        val layout = BarChartEngine.layout(data)
        assertEquals(5, layout.bars.size)
        assertTrue(layout.bars[4].isPartial)
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
    fun inverted_axis_faster_pace_gets_taller_bar() {
        // 페이스 250(빠름)…350(느림) 5막대 → 빠를수록 heightFraction이 커야 한다(축 반전).
        val data = BarChartData(
            listOf(250.0, 275.0, 300.0, 325.0, 350.0).map { SplitSample(1000.0, it) },
            splitDistanceMeters = 1000.0,
        )
        val bars = BarChartEngine.layout(data).bars
        assertTrue(bars[0].heightFraction > bars[1].heightFraction)
        assertTrue(bars[1].heightFraction > bars[2].heightFraction)
    }

    @Test
    fun inverted_axis_smallest_tick_at_top() {
        // yTicks는 값 오름차순으로 오는데, 반전 축에서는 값이 작을수록(빠를수록) position이 커야 한다(1.0=천장).
        val data = BarChartData(
            listOf(SplitSample(1000.0, 250.0), SplitSample(1000.0, 300.0), SplitSample(1000.0, 350.0)),
            splitDistanceMeters = 1000.0,
        )
        val ticks = BarChartEngine.layout(data).yTicks
        assertTrue(ticks.size >= 2)
        for (i in 1 until ticks.size) {
            assertTrue(ticks[i].value > ticks[i - 1].value)
            assertTrue(ticks[i].position < ticks[i - 1].position)
        }
        // 도메인 양끝 틱은 정확히 천장/바닥에 닿는다.
        assertEquals(1.0, ticks.first().position, 1e-9)
        assertEquals(0.0, ticks.last().position, 1e-9)
    }

    @Test
    fun inverted_axis_reference_line_matches_tick_scale() {
        // ref=평균. 반전 축에서 ref 위치는 같은 값의 틱 보간과 일치해야 한다: 1 - normalize(ref).
        // 평균(300)이 도메인 중앙에 오지 않도록 비대칭 값 사용 — 대칭이면 반전 전후가 같아 회귀를 못 잡는다.
        val data = BarChartData(
            listOf(SplitSample(1000.0, 250.0), SplitSample(1000.0, 250.0), SplitSample(1000.0, 400.0)),
            splitDistanceMeters = 1000.0,
        )
        val layout = BarChartEngine.layout(data)
        val lo = layout.yTicks.first().value
        val hi = layout.yTicks.last().value
        val ref = 300.0 // 시간가중 평균 = 900s / 3km
        val expected = 1.0 - (ref - lo) / (hi - lo)
        assertEquals(expected, layout.referenceLinePosition!!, 1e-9)
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
        // 5.5km를 1650s에 커버(=300s/km) → 1km full 5개 + 0.5km partial, 전부 300s/unit.
        val data = BarChartData(listOf(SplitSample(5500.0, 1650.0)), splitDistanceMeters = 1000.0)
        val bars = BarChartEngine.layout(data).bars
        assertEquals(6, bars.size)
        assertTrue(bars.take(5).none { it.isPartial })
        assertTrue(bars[5].isPartial)
        bars.forEach { assertEquals(300.0, it.value, 1e-6) }
    }

    @Test
    fun very_short_run_subdivides_to_min_bars() {
        // 0.3km(90s = 300s/km)만 있는 짧은 러닝 → 50m 버킷 6막대. 값은 여전히 sec/km(300).
        val layout = BarChartEngine.layout(
            BarChartData(listOf(SplitSample(300.0, 90.0)), 1000.0))
        assertEquals(6, layout.bars.size)
        layout.bars.forEach {
            assertEquals(300.0, it.value, 1e-6)
            assertTrue(it.heightFraction in 0.0..1.0)
        }
    }

    @Test
    fun short_run_keeps_pace_unit_while_bucket_shrinks() {
        // 0.64km를 8분(480s)에 → 750s/km. 버킷은 100m로 줄어도 value는 sec/km라야 한다.
        // 100m를 75s에 지나는 페이스이므로 100m 버킷 값도 750, sec/100m였다면 75가 나온다.
        val samples = List(64) { SplitSample(10.0, 7.5) } // 10m씩 7.5s = 750s/km
        val layout = BarChartEngine.layout(BarChartData(samples, splitDistanceMeters = 1000.0))
        assertEquals(7, layout.bars.size) // 온전 6 + 부분 1(40m)
        layout.bars.forEach { assertEquals(750.0, it.value, 1e-6) }
        assertTrue(layout.bars.last().isPartial)
    }

    @Test
    fun distance_mode_end_distance_is_cumulative_meters() {
        // 위와 같은 0.64km → 100m 버킷: 100,200,…,600 + 부분은 총거리 640
        val samples = List(64) { SplitSample(10.0, 7.5) }
        val ends = BarChartEngine.layout(
            BarChartData(samples, splitDistanceMeters = 1000.0)).bars.map { it.endDistanceMeters!! }
        val expected = listOf(100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 640.0)
        assertEquals(expected.size, ends.size)
        for (i in expected.indices) assertEquals(expected[i], ends[i], 1e-6)
    }

    @Test
    fun distance_mode_last_partial_end_snaps_to_total_distance() {
        // 워치 총거리(totalDistanceMeters)가 유효 델타 합보다 크면 — 무효 델타 필터로 거리가
        // 증발한 기록 — 마지막 부분 스플릿 끝을 총거리로 스냅해 요약 수치와 라벨을 맞춘다.
        val samples = List(64) { SplitSample(10.0, 7.5) } // 유효 합 640m
        val layout = BarChartEngine.layout(BarChartData(
            samples, splitDistanceMeters = 1000.0,
            totalDurationSeconds = 480.0, totalDistanceMeters = 690.0,
        ))
        val ends = layout.bars.map { it.endDistanceMeters!! }
        val expected = listOf(100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 690.0)
        assertEquals(expected.size, ends.size)
        for (i in expected.indices) assertEquals(expected[i], ends[i], 1e-6)
        // 스냅은 라벨 값만 — 페이스는 유효 구간 기준 그대로.
        layout.bars.forEach { assertEquals(750.0, it.value, 1e-6) }
    }

    @Test
    fun distance_mode_last_partial_end_keeps_covered_when_total_smaller() {
        // 총거리가 유효 합보다 작으면(역방향 불일치) 스냅하지 않는다 — 차트 내 정합 우선.
        val samples = List(64) { SplitSample(10.0, 7.5) }
        val ends = BarChartEngine.layout(BarChartData(
            samples, splitDistanceMeters = 1000.0,
            totalDurationSeconds = 480.0, totalDistanceMeters = 600.0,
        )).bars.map { it.endDistanceMeters!! }
        assertEquals(640.0, ends.last(), 1e-6)
    }

    @Test
    fun distance_mode_full_last_bar_never_snaps() {
        // 마지막 막대가 온전 스플릿이면(부분 없음) 버킷 경계가 곧 라벨 — 총거리로 덮지 않는다.
        val samples = List(60) { SplitSample(10.0, 7.5) } // 정확히 600m = 100m 버킷 6개
        val ends = BarChartEngine.layout(BarChartData(
            samples, splitDistanceMeters = 1000.0,
            totalDurationSeconds = 450.0, totalDistanceMeters = 690.0,
        )).bars.map { it.endDistanceMeters!! }
        assertEquals(600.0, ends.last(), 1e-6)
    }

    @Test
    fun time_mode_last_partial_end_snaps_to_total_duration() {
        // 기록 시간(일시정지 포함)이 유효 델타 합보다 크면 — 워치 13:53 vs 유효 10:53 —
        // 마지막 부분 버킷 끝을 총 시간으로 스냅해 요약 수치와 라벨을 맞춘다(거리 스냅과 대칭).
        val layout = BarChartEngine.layout(BarChartData(
            evenTimeSamples(653, 300.0), splitDistanceMeters = 1000.0,
            splitTimeSeconds = 120.0,
            totalDurationSeconds = 833.0, totalDistanceMeters = 890.0,
        ))
        val ends = layout.bars.map { it.endSeconds!! }
        assertEquals(listOf(120.0, 240.0, 360.0, 480.0, 600.0, 833.0), ends)
        assertEquals(14, layout.bars.last().endMinutes) // round(833/60) — 스냅값 기준
        assertTrue(layout.bars.last().isPartial)
    }

    @Test
    fun time_mode_last_partial_end_keeps_elapsed_when_total_smaller() {
        // 총 시간이 유효 합보다 작으면(역방향 불일치) 스냅하지 않는다 — 차트 내 정합 우선.
        val layout = BarChartEngine.layout(BarChartData(
            evenTimeSamples(653, 300.0), splitDistanceMeters = 1000.0,
            splitTimeSeconds = 120.0,
            totalDurationSeconds = 600.0,
        ))
        assertEquals(653.0, layout.bars.last().endSeconds!!, 1e-6)
    }

    @Test
    fun time_mode_end_distance_null() {
        val data = BarChartData(
            evenTimeSamples(720, 300.0), splitDistanceMeters = 1000.0,
            splitTimeSeconds = 120.0,
        )
        assertTrue(BarChartEngine.layout(data).bars.all { it.endDistanceMeters == null })
    }

    // MARK: chooseDistanceBucketMeters

    @Test
    fun distance_bucket_stays_unit_when_five_or_more_bars() {
        // 5.0km → 1km 5막대, 4.2km → 4 온전+부분 = 5막대. 둘 다 하향 없음.
        assertEquals(1000.0, BarChartEngine.chooseDistanceBucketMeters(5000.0, 1000.0), 1e-9)
        assertEquals(1000.0, BarChartEngine.chooseDistanceBucketMeters(4200.0, 1000.0), 1e-9)
    }

    @Test
    fun distance_bucket_steps_down_until_five_bars() {
        // 4.0km 정확: 부분 스플릿이 없어 4막대 → 500m(8막대)
        assertEquals(500.0, BarChartEngine.chooseDistanceBucketMeters(4000.0, 1000.0), 1e-9)
        // 3.2km: 1km→4, 500m→7 → 500m
        assertEquals(500.0, BarChartEngine.chooseDistanceBucketMeters(3200.0, 1000.0), 1e-9)
        // 0.64km: 1km→1, 500m→2, 200m→4, 100m→7 → 100m
        assertEquals(100.0, BarChartEngine.chooseDistanceBucketMeters(640.0, 1000.0), 1e-9)
    }

    @Test
    fun distance_bucket_floor_is_one_twentieth_unit() {
        // 0.2km: 50m로도 4막대 → 더 못 내려가고 하한 유지
        assertEquals(50.0, BarChartEngine.chooseDistanceBucketMeters(200.0, 1000.0), 1e-9)
    }

    @Test
    fun distance_bucket_scales_with_mile_unit() {
        // 1.5마일: 1마일→2, 1/2→3, 1/5→8 → 마일/5
        assertEquals(1609.344 / 5.0, BarChartEngine.chooseDistanceBucketMeters(2414.016, 1609.344), 1e-9)
        assertEquals(1609.344, BarChartEngine.chooseDistanceBucketMeters(1609.344 * 5, 1609.344), 1e-9)
    }

    @Test
    fun distance_bucket_degenerate_total_returns_unit() {
        // 무효 총거리는 하향할 근거가 없다 — 단위 그대로(막대도 안 그려진다)
        assertEquals(1000.0, BarChartEngine.chooseDistanceBucketMeters(0.0, 1000.0), 1e-9)
        assertEquals(1000.0, BarChartEngine.chooseDistanceBucketMeters(-5.0, 1000.0), 1e-9)
        assertEquals(1000.0, BarChartEngine.chooseDistanceBucketMeters(Double.NaN, 1000.0), 1e-9)
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

    @Test
    fun bucket_selection_drops_below_one_minute_for_very_short_run() {
        // 3분(180s): 1분→3막대(5개 미만) → 30초(6막대)
        assertEquals(30.0, BarChartEngine.chooseTimeBucketSeconds(180.0), 1e-9)
        // 2분(120s): 1분→2, 30초→4 모두 미달 → 15초(8막대)
        assertEquals(15.0, BarChartEngine.chooseTimeBucketSeconds(120.0), 1e-9)
        // 1분(60s): 15초로도 4막대 → 하한 유지
        assertEquals(15.0, BarChartEngine.chooseTimeBucketSeconds(60.0), 1e-9)
    }

    @Test
    fun bucket_selection_five_minute_run_unchanged() {
        // 회귀 가드: 5분은 1분 버킷 5막대 — 30초 후보가 생겨도 기존 선택이 이겨야 한다.
        assertEquals(60.0, BarChartEngine.chooseTimeBucketSeconds(300.0), 1e-9)
        // 4분(240s)은 1분→4막대라 하향 대상 → 30초(8막대)
        assertEquals(30.0, BarChartEngine.chooseTimeBucketSeconds(240.0), 1e-9)
    }

    @Test
    fun bucket_selection_non_finite_duration_degrades_coarsely() {
        // 오염된 총시간(워치 임포트 오류 등)이 15초 최소로 떨어지면 유한한 샘플이 수백 막대로
        // 조각난다 — 0.41.0 이전 동작 유지: +Inf는 최대 캡(600s), NaN/-Inf는 최소 분 후보(60s).
        assertEquals(600.0, BarChartEngine.chooseTimeBucketSeconds(Double.POSITIVE_INFINITY), 1e-9)
        assertEquals(60.0, BarChartEngine.chooseTimeBucketSeconds(Double.NaN), 1e-9)
        assertEquals(60.0, BarChartEngine.chooseTimeBucketSeconds(Double.NEGATIVE_INFINITY), 1e-9)
        // 0초는 골든 고정(running_0 → 15) — 가드가 유한 0을 건드리면 안 된다.
        assertEquals(15.0, BarChartEngine.chooseTimeBucketSeconds(0.0), 1e-9)
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
    fun time_mode_end_seconds_are_cumulative_and_unrounded() {
        // 180s를 30초 버킷으로 → endSeconds = 30,60,90,120,150,180 (분 반올림이면 1,1,2,2,3,3로 뭉갠다)
        val data = BarChartData(
            evenTimeSamples(180, 300.0), splitDistanceMeters = 1000.0,
            splitTimeSeconds = 30.0,
        )
        val bars = BarChartEngine.layout(data).bars
        assertEquals(listOf(30.0, 60.0, 90.0, 120.0, 150.0, 180.0), bars.map { it.endSeconds!! })
        assertEquals(listOf(1, 1, 2, 2, 3, 3), bars.map { it.endMinutes })
    }

    @Test
    fun distance_mode_end_seconds_null() {
        val data = BarChartData(evenSamples(5, 300.0), splitDistanceMeters = 1000.0)
        assertTrue(BarChartEngine.layout(data).bars.all { it.endSeconds == null })
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
