package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeartRateZoneEngineTest {
    private fun s(hr: Double, dt: Double) = HeartRateZoneSample(hr, dt)

    @Test fun zone_boundaries_by_max() {
        // max=200 → 경계 100/120/140/160/180. 100 미만 제외.
        val r = HeartRateZoneEngine.calculate(
            listOf(s(90.0, 10.0), s(110.0, 10.0), s(130.0, 10.0),
                   s(150.0, 10.0), s(170.0, 10.0), s(190.0, 10.0)), 200)
        assertEquals(listOf(10.0, 10.0, 10.0, 10.0, 10.0), r)
    }

    @Test fun time_weighted_accumulation() {
        val r = HeartRateZoneEngine.calculate(listOf(s(180.0, 5.0), s(190.0, 15.0), s(110.0, 3.0)), 200)
        assertEquals(20.0, r[4], 1e-9)
        assertEquals(3.0, r[0], 1e-9)
    }

    @Test fun lower_edge_inclusive() {
        val r = HeartRateZoneEngine.calculate(
            listOf(s(100.0, 1.0), s(120.0, 1.0), s(140.0, 1.0), s(160.0, 1.0), s(180.0, 1.0)), 200)
        assertEquals(listOf(1.0, 1.0, 1.0, 1.0, 1.0), r)
    }

    @Test fun below_fifty_percent_excluded() {
        val r = HeartRateZoneEngine.calculate(listOf(s(50.0, 10.0), s(99.0, 10.0), s(150.0, 5.0)), 200)
        assertEquals(listOf(0.0, 0.0, 5.0, 0.0, 0.0), r)
    }

    @Test fun non_positive_hr_or_dt_excluded() {
        val r = HeartRateZoneEngine.calculate(listOf(s(0.0, 10.0), s(-5.0, 10.0), s(150.0, 10.0), s(150.0, 0.0)), 200)
        assertEquals(10.0, r[2], 1e-9)
        assertEquals(10.0, r.sum(), 1e-9)
    }

    @Test fun zero_max_returns_all_zero() {
        assertEquals(listOf(0.0, 0.0, 0.0, 0.0, 0.0), HeartRateZoneEngine.calculate(listOf(s(150.0, 10.0)), 0))
    }

    @Test fun bpm_ranges_match_old_screen() {
        // max=184: 92/110, 111/128, 129/147, 148/165, 166/nil
        val r = HeartRateZoneEngine.zoneBpmRanges(184)
        assertEquals(5, r.size)
        assertEquals(ZoneBpmRange(92, 110), r[0])
        assertEquals(ZoneBpmRange(111, 128), r[1])
        assertEquals(ZoneBpmRange(129, 147), r[2])
        assertEquals(ZoneBpmRange(148, 165), r[3])
        assertEquals(166, r[4].lower)
        assertNull(r[4].upper)
    }

    @Test fun bpm_ranges_empty_when_no_max() {
        assertTrue(HeartRateZoneEngine.zoneBpmRanges(0).isEmpty())
    }

    @Test fun bpm_ranges_consistent_with_calculate_at_integer_boundaries() {
        // max=200: 경계 정수(100/120/…). 라벨 하한 bpm이 그 존으로 집계돼야 함(불변식).
        val ranges = HeartRateZoneEngine.zoneBpmRanges(200)
        assertEquals(ZoneBpmRange(100, 119), ranges[0])
        assertEquals(120, ranges[1].lower)
        ranges.forEachIndexed { zoneIndex, range ->
            val r = HeartRateZoneEngine.calculate(listOf(s(range.lower.toDouble(), 1.0)), 200)
            assertEquals(1.0, r[zoneIndex], 1e-9)
        }
    }
}
