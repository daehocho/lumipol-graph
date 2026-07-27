package com.lumipol.graph

import com.lumipol.graph.model.BuildOptions
import com.lumipol.graph.model.DistanceUnit
import com.lumipol.graph.model.RawTrackSample
import com.lumipol.graph.model.RunTotals
import com.lumipol.graph.model.XMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** C1 — 원천 전처리(페이스 D2 병합식·x·스플릿·HR존 dt D11)의 코어 확정 규칙. */
class TrackChartBuilderTest {

    private val totals = RunTotals(sumDistanceMeters = 3000.0, runningSeconds = 900.0)
    private val optionsKm = BuildOptions(DistanceUnit.KILOMETERS, XMode.DISTANCE)

    /** AOS형 원천(누적만 보유). */
    private fun cumulative(distM: Double, sec: Double, hr: Double? = null) = RawTrackSample(
        cumulativeDistanceMeters = distM, deltaDistanceMeters = null,
        cumulativeSeconds = sec, deltaSeconds = null,
        speedMps = null, latitude = null, longitude = null,
        heartRate = hr, cadence = null, altitude = null,
    )

    /** iOS형 원천(델타 보유 + 워치 speed). */
    private fun delta(distM: Double, sec: Double, speed: Double? = null) = RawTrackSample(
        cumulativeDistanceMeters = null, deltaDistanceMeters = distM,
        cumulativeSeconds = null, deltaSeconds = sec,
        speedMps = speed, latitude = null, longitude = null,
        heartRate = null, cadence = null, altitude = null,
    )

    @Test
    fun gps_pace_uses_delta_over_time_with_gate() {
        // 200m/60s = 3.333m/s = 12km/h → 유효. 페이스 = 1000/3.333… = 300s/km.
        val input = TrackChartBuilder.paceInput(
            listOf(cumulative(200.0, 60.0), cumulative(400.0, 120.0)),
            totals, optionsKm,
        )
        assertEquals(2, input.points.size)
        assertEquals(300.0, input.points[1].paceSeconds, 1e-9)
        // x = 누적 km.
        assertEquals(0.4, input.points[1].x, 1e-12)
    }

    @Test
    fun speed_gate_rejects_outliers_as_invalid() {
        // 5m/60s = 0.3km/h(< 1) → 무효 0. 800m/60s = 48km/h(> 41) → 무효 0.
        val input = TrackChartBuilder.paceInput(
            listOf(cumulative(5.0, 60.0), cumulative(805.0, 120.0)),
            totals, optionsKm,
        )
        assertEquals(0.0, input.points[0].paceSeconds)
        assertEquals(0.0, input.points[1].paceSeconds)
    }

    @Test
    fun watch_speed_takes_priority_and_is_gated() {
        val opts = BuildOptions(DistanceUnit.KILOMETERS, XMode.DISTANCE, useWatchSpeed = true)
        val input = TrackChartBuilder.paceInput(
            listOf(
                delta(200.0, 60.0, speed = 2.5),   // 9km/h → 1000/2.5 = 400s/km
                delta(200.0, 60.0, speed = 20.0),  // 72km/h → 게이트 무효
                delta(200.0, 60.0, speed = null),  // 워치 값 없음 → 무효
            ),
            totals, opts,
        )
        assertEquals(400.0, input.points[0].paceSeconds, 1e-9)
        assertEquals(0.0, input.points[1].paceSeconds)
        assertEquals(0.0, input.points[2].paceSeconds)
    }

    @Test
    fun mile_unit_scales_pace_and_x_from_single_constant() {
        val opts = BuildOptions(DistanceUnit.MILES, XMode.DISTANCE)
        val input = TrackChartBuilder.paceInput(
            listOf(cumulative(1609.344, 480.0)), totals, opts,
        )
        // 1마일을 480s → 480s/mile. 속도 12.07km/h → 게이트 통과.
        assertEquals(480.0, input.points[0].paceSeconds, 1e-9)
        assertEquals(1.0, input.points[0].x, 1e-12)
    }

    @Test
    fun time_mode_x_is_minutes() {
        val opts = BuildOptions(DistanceUnit.KILOMETERS, XMode.TIME)
        val input = TrackChartBuilder.paceInput(listOf(cumulative(200.0, 90.0)), totals, opts)
        assertEquals(1.5, input.points[0].x, 1e-12)
    }

    @Test
    fun haversine_fallback_kicks_in_when_cumulative_delta_is_zero() {
        // 위도 0.0018° ≈ 200m (경도 0, 적도) — 누적 거리 정지 상태에서 좌표만 이동.
        val a = RawTrackSample(
            cumulativeDistanceMeters = 500.0, deltaDistanceMeters = null,
            cumulativeSeconds = 60.0, deltaSeconds = null, speedMps = null,
            latitude = 0.0, longitude = 0.0, heartRate = null, cadence = null, altitude = null,
        )
        val b = RawTrackSample(
            cumulativeDistanceMeters = 500.0, deltaDistanceMeters = null, // 델타 0 → 폴백
            cumulativeSeconds = 120.0, deltaSeconds = null, speedMps = null,
            latitude = 0.0018, longitude = 0.0, heartRate = null, cadence = null, altitude = null,
        )
        val input = TrackChartBuilder.paceInput(listOf(a, b), totals, optionsKm)
        val pace = input.points[1].paceSeconds
        // ≈200m/60s = 12km/h → 유효, 페이스 ≈ 300s/km(±2% — 구면 근사 오차).
        assertTrue(pace in 290.0..310.0, "haversine 폴백 페이스: $pace")
    }

    @Test
    fun invalid_totals_produce_empty_points() {
        val input = TrackChartBuilder.paceInput(
            listOf(cumulative(200.0, 60.0)), RunTotals(0.0, 900.0), optionsKm,
        )
        assertTrue(input.points.isEmpty())
    }

    @Test
    fun split_samples_reconstruct_deltas_and_drop_invalid() {
        // 누적형: 첫 행 = 시작 구간(prev=0 시드). 역전 구간(델타 ≤ 0)은 제외.
        val splits = TrackChartBuilder.splitSamples(
            listOf(cumulative(1000.0, 300.0), cumulative(2000.0, 610.0), cumulative(2000.0, 620.0)),
        )
        assertEquals(2, splits.size)
        assertEquals(1000.0, splits[0].distanceMeters)
        assertEquals(300.0, splits[0].timeSeconds)
        assertEquals(310.0, splits[1].timeSeconds)
    }

    @Test
    fun split_samples_pass_through_stored_deltas() {
        val splits = TrackChartBuilder.splitSamples(
            listOf(delta(12.0, 5.0), delta(0.0, 5.0), delta(11.0, 0.0)),
        )
        assertEquals(listOf(12.0), splits.map { it.distanceMeters })
    }

    @Test
    fun zone_samples_use_cumulative_delta_with_clamp() {
        val zones = TrackChartBuilder.zoneSamples(
            listOf(cumulative(0.0, 10.0, hr = 150.0), cumulative(0.0, 8.0, hr = 155.0), cumulative(0.0, 25.0, hr = 160.0)),
        )
        assertEquals(listOf(10.0, 0.0, 15.0), zones.map { it.timeInterval }) // 역전은 0, prev는 최대 유지
        assertEquals(listOf(150.0, 155.0, 160.0), zones.map { it.heartRate })
    }

    @Test
    fun zone_samples_fall_back_to_point_deltas_for_legacy_records() {
        // D11: 누적 운동시간이 전부 0/누락(2021 이전 기록) → per-point 델타 폴백.
        val legacy = listOf(
            RawTrackSample(null, 10.0, null, 12.0, null, null, null, 150.0, null, null),
            RawTrackSample(null, 10.0, null, -3.0, null, null, null, 155.0, null, null),
        )
        val zones = TrackChartBuilder.zoneSamples(legacy)
        assertEquals(listOf(12.0, 0.0), zones.map { it.timeInterval })
    }

    @Test
    fun sanitized_factory_absorbs_sentinels() {
        val s = RawTrackSample.sanitized(
            cumulativeDistanceMeters = 100.0, deltaDistanceMeters = null,
            cumulativeSeconds = 10.0, deltaSeconds = null, speedMps = null,
            latitude = null, longitude = null,
            rawHeartRate = 0.0, rawCadence = 300.0, rawAltitude = -100.0,
        )
        assertNull(s.heartRate)                       // 0 = 결측
        assertEquals(250.0, s.cadence)                // 상한 클램프
        assertNull(s.altitude)                        // ≤ -100 = 미측정
    }
}
