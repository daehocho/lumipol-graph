package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PaceSeriesEngineTest {
    // 균일 10'00"/km(=600s/km): x는 앱이 계산한 값이라 여기선 누적 km를 직접 부여.
    private fun evenPoints(n: Int, paceSec: Double = 600.0, hr: Double = 150.0,
                           cad: Double = 0.0, altRamp: Boolean = false): List<PaceSamplePoint> =
        (0 until n).map { i ->
            PaceSamplePoint(
                x = (i + 1) * 0.01, paceSeconds = paceSec, heartRate = hr,
                cadence = cad, altitude = if (altRamp) i.toDouble() else 0.0)
        }

    @Test fun even_run_all_valid_pace_in_minutes() {
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(100), 600.0, 1000.0))
        assertEquals(100, r.validPaceCount)
        // y = 600/60 = 10.0 분
        r.pace.forEach { assertEquals(10.0, it.y, 1e-9) }
    }

    @Test fun filters_out_of_bounds_pace() {
        // 하한 120s 미만·상한(avg+600) 초과는 무효. avg=600/(?/1000)… 여기선 50 유효 + 스파이크 2.
        val pts = (0 until 50).map { PaceSamplePoint((it + 1) * 0.01, 600.0, 0.0, 0.0, 0.0) } +
            PaceSamplePoint(0.51, 30.0, 0.0, 0.0, 0.0) +    // 30s/km → <120 컷
            PaceSamplePoint(0.52, 1200.0, 0.0, 0.0, 0.0)    // 1200s/km → >avg+600 컷
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 312.5, 520.0))
        assertEquals(50, r.validPaceCount)
    }

    @Test fun slow_outlier_cap_cuts_isolated_spike() {
        // 100포인트 600s + 900s 스파이크 2개. p95≈600, ×1.25=750 → 900 컷.
        val pts = (0 until 100).map { PaceSamplePoint((it + 1) * 0.01, 600.0, 0.0, 0.0, 0.0) } +
            PaceSamplePoint(1.01, 900.0, 0.0, 0.0, 0.0) + PaceSamplePoint(1.02, 900.0, 0.0, 0.0, 0.0)
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 618.0, 1020.0))
        assertEquals(100, r.validPaceCount)
        r.pace.forEach { assertTrue(it.y < 12.0) } // 720s=12분 미만
    }

    @Test fun skips_cap_under_min_samples() {
        // 표본 15+1: 20 미만이라 퍼센타일 컷 없음. 720s(=avg+? 통과)까지 유효.
        val pts = (0 until 15).map { PaceSamplePoint((it + 1) * 0.01, 600.0, 0.0, 0.0, 0.0) } +
            PaceSamplePoint(0.16, 720.0, 0.0, 0.0, 0.0)
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 97.2, 160.0))
        assertEquals(16, r.validPaceCount)
    }

    @Test fun downsample_caps_near_3000() {
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(6000), 3600.0, 60000.0))
        assertEquals(6000, r.validPaceCount)
        assertEquals(3000, r.pace.size) // skip = 6000/3000 = 2
    }

    @Test fun heart_and_cadence_carry_forward_and_empty_when_all_zero() {
        val pts = listOf(
            PaceSamplePoint(0.01, 600.0, 150.0, 180.0, 0.0),
            PaceSamplePoint(0.02, 600.0, 0.0, 0.0, 0.0),   // 결측 → 직전 150/180 승계
            PaceSamplePoint(0.03, 600.0, 160.0, 190.0, 0.0),
        )
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 18.0, 30.0))
        assertEquals(listOf(150.0, 150.0, 160.0), r.heart.map { it.y })
        assertEquals(listOf(180.0, 180.0, 190.0), r.cadence.map { it.y })
        // 전부 0이면 빈 리스트
        val z = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(10, hr = 0.0, cad = 0.0), 60.0, 100.0))
        assertTrue(z.heart.isEmpty() && z.cadence.isEmpty())
    }

    @Test fun altitude_area_when_varies_else_null() {
        val varies = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(100, altRamp = true), 600.0, 1000.0))
        assertNotNull(varies.altitudeArea)
        assertTrue(varies.altitudeArea!!.size >= 2)
        val flat = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(100), 600.0, 1000.0))
        assertNull(flat.altitudeArea) // 전부 0 → 평지
    }

    @Test fun empty_when_no_distance_or_time() {
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(10), 0.0, 0.0))
        assertTrue(r.pace.isEmpty() && r.heart.isEmpty() && r.cadence.isEmpty())
        assertNull(r.altitudeArea)
        assertEquals(0.0, r.bestPaceSeconds, 1e-9)
        assertEquals(0, r.validPaceCount)
    }

    @Test fun best_pace_is_min_valid_seconds() {
        val pts = listOf(
            PaceSamplePoint(0.01, 600.0, 0.0, 0.0, 0.0),
            PaceSamplePoint(0.02, 300.0, 0.0, 0.0, 0.0), // 최고(최소 초)
            PaceSamplePoint(0.03, 450.0, 0.0, 0.0, 0.0),
        )
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 18.0, 30.0))
        assertEquals(300.0, r.bestPaceSeconds, 1e-9)
    }
}
