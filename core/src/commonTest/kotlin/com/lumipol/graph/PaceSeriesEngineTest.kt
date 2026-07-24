package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PaceSeriesEngineTest {
    // 균일 10'00"/km(=600s/km): x는 앱이 계산한 값이라 여기선 누적 km를 직접 부여.
    // 결측은 null(센티널 0 아님) — 코어 계약이 nullable이다.
    private fun evenPoints(n: Int, paceSec: Double = 600.0, hr: Double? = 150.0,
                           cad: Double? = null, altRamp: Boolean = false): List<PaceSamplePoint> =
        (0 until n).map { i ->
            PaceSamplePoint(
                x = (i + 1) * 0.01, paceSeconds = paceSec, heartRate = hr,
                cadence = cad, altitude = if (altRamp) i.toDouble() else null)
        }

    private fun pacePoint(x: Double, paceSec: Double) =
        PaceSamplePoint(x, paceSec, heartRate = null, cadence = null, altitude = null)

    @Test fun even_run_all_valid_pace_in_minutes() {
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(100), 600.0, 1000.0))
        assertEquals(100, r.validPaceCount)
        // y = 600/60 = 10.0 분
        r.pace.forEach { assertEquals(10.0, it.y, 1e-9) }
    }

    @Test fun filters_out_of_bounds_pace() {
        // 하한 120s 미만·상한(avg+600) 초과는 무효. avg=600/(?/1000)… 여기선 50 유효 + 스파이크 2.
        val pts = (0 until 50).map { pacePoint((it + 1) * 0.01, 600.0) } +
            pacePoint(0.51, 30.0) +    // 30s/km → <120 컷
            pacePoint(0.52, 1200.0)    // 1200s/km → >avg+600 컷
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 312.5, 520.0))
        assertEquals(50, r.validPaceCount)
    }

    @Test fun slow_outlier_cap_cuts_isolated_spike() {
        // 100포인트 600s + 900s 스파이크 2개. p95≈600, ×1.25=750 → 900 컷.
        val pts = (0 until 100).map { pacePoint((it + 1) * 0.01, 600.0) } +
            pacePoint(1.01, 900.0) + pacePoint(1.02, 900.0)
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 618.0, 1020.0))
        assertEquals(100, r.validPaceCount)
        r.pace.forEach { assertTrue(it.y < 12.0) } // 720s=12분 미만
    }

    @Test fun skips_cap_under_min_samples() {
        // 표본 15+1: 20 미만이라 퍼센타일 컷 없음. 720s(=avg+? 통과)까지 유효.
        val pts = (0 until 15).map { pacePoint((it + 1) * 0.01, 600.0) } + pacePoint(0.16, 720.0)
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 97.2, 160.0))
        assertEquals(16, r.validPaceCount)
    }

    @Test fun downsample_caps_near_3000() {
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(6000), 3600.0, 60000.0))
        assertEquals(6000, r.validPaceCount)
        assertEquals(3000, r.pace.size) // skip = 6000/3000 = 2
    }

    @Test fun heart_and_cadence_carry_forward_and_empty_when_all_missing() {
        val pts = listOf(
            PaceSamplePoint(0.01, 600.0, 150.0, 180.0, null),
            PaceSamplePoint(0.02, 600.0, null, null, null),   // 결측 → 직전 150/180 승계
            PaceSamplePoint(0.03, 600.0, 160.0, 190.0, null),
        )
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 18.0, 30.0))
        assertEquals(listOf(150.0, 150.0, 160.0), r.heart.map { it.y })
        assertEquals(listOf(180.0, 180.0, 190.0), r.cadence.map { it.y })
        // 전부 결측이면 빈 리스트
        val z = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(10, hr = null, cad = null), 60.0, 100.0))
        assertTrue(z.heart.isEmpty() && z.cadence.isEmpty())
    }

    @Test fun zero_is_a_real_measurement_not_a_sentinel() {
        // 구 계약에선 0이 "미측정"이라 통째로 버려졌다. 이제 null만 결측이므로 0도 데이터다.
        val pts = (0 until 20).map { PaceSamplePoint((it + 1) * 0.01, 600.0, 0.0, 0.0, null) }
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 120.0, 200.0))
        assertEquals(20, r.heart.size)
        assertTrue(PaceSeriesId.HEART in r.availableSeries)
        assertTrue(PaceSeriesId.CADENCE in r.availableSeries)
    }

    @Test fun leading_missing_backfills_from_first_valid_for_every_metric() {
        // 세 지표가 같은 규칙이어야 한다. 한쪽만 소급하면 나머지는 앞 구간이 0으로 꺼지는데,
        // 0은 이제 실측값이라 소비 앱이 "채워진 구멍"과 구분할 수 없다.
        val pts = listOf(
            PaceSamplePoint(0.01, 600.0, null, null, null),
            PaceSamplePoint(0.02, 600.0, 150.0, 80.0, 30.0),
            PaceSamplePoint(0.03, 600.0, 152.0, 82.0, 40.0),
        )
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 18.0, 30.0))
        assertEquals(listOf(150.0, 150.0, 152.0), r.heart.map { it.y })
        assertEquals(listOf(80.0, 80.0, 82.0), r.cadence.map { it.y })
        assertEquals(listOf(30.0, 30.0, 40.0), r.altitudeArea!!.map { it.y })
    }

    @Test fun leading_zero_measurement_is_not_backfilled() {
        // 0은 실측값이므로 소급 대상이 아니다 — 승계는 null에만 적용된다.
        val pts = listOf(
            PaceSamplePoint(0.01, 600.0, 0.0, null, null),
            PaceSamplePoint(0.02, 600.0, null, null, null),
            PaceSamplePoint(0.03, 600.0, 150.0, null, null),
        )
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 18.0, 30.0))
        assertEquals(listOf(0.0, 0.0, 150.0), r.heart.map { it.y })
    }

    @Test fun altitude_area_when_measured_else_null() {
        val varies = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(100, altRamp = true), 600.0, 1000.0))
        assertNotNull(varies.altitudeArea)
        assertTrue(varies.altitudeArea!!.size >= 2)
        val missing = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(100), 600.0, 1000.0))
        assertNull(missing.altitudeArea) // 전부 null → 미측정
        assertFalse(PaceSeriesId.ALTITUDE in missing.availableSeries)
    }

    @Test fun altitude_area_kept_on_flat_course() {
        // 평지 컷 없음 — 실측 고도가 있으면 고저차가 0이어도 실루엣을 낸다.
        // "얼마나 크게 그릴지"는 렌더의 minSpan이 맡는다(heightFractions).
        val pts = (0 until 100).map { i ->
            PaceSamplePoint((i + 1) * 0.01, 600.0, 150.0, null, 10.0)
        }
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 600.0, 1000.0))
        assertNotNull(r.altitudeArea)
        assertTrue(PaceSeriesId.ALTITUDE in r.availableSeries)
    }

    @Test fun available_series_reports_measured_metrics() {
        val r = PaceSeriesEngine.preprocess(
            PaceSeriesInput(evenPoints(100, cad = 180.0, altRamp = true), 600.0, 1000.0))
        assertEquals(
            setOf(PaceSeriesId.PACE, PaceSeriesId.HEART, PaceSeriesId.CADENCE, PaceSeriesId.ALTITUDE),
            r.availableSeries)
    }

    @Test fun pace_not_available_under_min_valid_count() {
        // 유효 페이스 10개 이하는 지표로 치지 않는다(구 앱 규칙 "validPaceCount > 10" 이관).
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(10), 60.0, 100.0))
        assertEquals(10, r.validPaceCount)
        assertFalse(PaceSeriesId.PACE in r.availableSeries)
        // 미가용이면 필드도 빈다 — availableSeries를 안 보고 pace만 읽어도 어긋나지 않는다.
        assertTrue(r.pace.isEmpty())
        // 집계값은 그대로: 페이스를 라인으로 못 그려도 최고 기록 표시는 유효하다.
        assertEquals(600.0, r.bestPaceSeconds, 1e-9)
        // 경계: 11개면 노출
        val ok = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(11), 66.0, 110.0))
        assertEquals(11, ok.validPaceCount)
        assertTrue(PaceSeriesId.PACE in ok.availableSeries)
        assertEquals(11, ok.pace.size)
    }

    @Test fun series_fields_agree_with_available_series() {
        // 계약: 가용 ⟺ 필드 비어있지 않음. 네 지표 모두.
        val inputs = listOf(
            PaceSeriesInput(evenPoints(100, cad = 180.0, altRamp = true), 600.0, 1000.0),
            PaceSeriesInput(evenPoints(100, hr = null), 600.0, 1000.0),
            PaceSeriesInput(evenPoints(10), 60.0, 100.0),          // 페이스 임계 미달
            PaceSeriesInput(evenPoints(10), 0.0, 0.0),             // 조기 반환
        )
        for (input in inputs) {
            val r = PaceSeriesEngine.preprocess(input)
            assertEquals(PaceSeriesId.PACE in r.availableSeries, r.pace.isNotEmpty())
            assertEquals(PaceSeriesId.HEART in r.availableSeries, r.heart.isNotEmpty())
            assertEquals(PaceSeriesId.CADENCE in r.availableSeries, r.cadence.isNotEmpty())
            assertEquals(PaceSeriesId.ALTITUDE in r.availableSeries, r.altitudeArea != null)
        }
    }

    @Test fun empty_when_no_distance_or_time() {
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(evenPoints(10), 0.0, 0.0))
        assertTrue(r.pace.isEmpty() && r.heart.isEmpty() && r.cadence.isEmpty())
        assertNull(r.altitudeArea)
        assertEquals(0.0, r.bestPaceSeconds, 1e-9)
        assertEquals(0, r.validPaceCount)
        assertTrue(r.availableSeries.isEmpty())
    }

    @Test fun best_pace_is_min_valid_seconds() {
        val pts = listOf(
            pacePoint(0.01, 600.0),
            pacePoint(0.02, 300.0), // 최고(최소 초)
            pacePoint(0.03, 450.0),
        )
        val r = PaceSeriesEngine.preprocess(PaceSeriesInput(pts, 18.0, 30.0))
        assertEquals(300.0, r.bestPaceSeconds, 1e-9)
    }
}
