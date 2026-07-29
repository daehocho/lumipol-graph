package com.lumipol.graph

import com.lumipol.graph.model.ChartConfig
import com.lumipol.graph.model.XMode
import kotlin.test.Test
import kotlin.test.assertEquals

/** C2 — 표시 문자열 규칙(D1: iOS 표기 + 99분 상한) + C5 segmentCount(D4). */
class ChartFormatTest {

    @Test
    fun pace_uses_ios_notation() {
        assertEquals("4'30\"", ChartFormat.pace(270.0))
        assertEquals("5'05\"", ChartFormat.pace(305.9))   // 초 절삭
        assertEquals("98'59\"", ChartFormat.pace(5939.9))
    }

    @Test
    fun pace_invalid_cases() {
        assertEquals("-'--\"", ChartFormat.paceInvalid())
        assertEquals("-'--\"", ChartFormat.pace(0.0))
        assertEquals("-'--\"", ChartFormat.pace(5.9))        // 하한(6s) 미만
        assertEquals("-'--\"", ChartFormat.pace(5940.0))     // 99분 상한(D1)
        assertEquals("-'--\"", ChartFormat.pace(Double.NaN))
        assertEquals("-'--\"", ChartFormat.pace(Double.POSITIVE_INFINITY))
    }

    @Test
    fun duration_rules_match_both_apps() {
        assertEquals("00:00", ChartFormat.duration(0.0))
        assertEquals("00:00", ChartFormat.duration(Double.NaN))
        assertEquals("05:07", ChartFormat.duration(307.9))
        assertEquals("1:01:05", ChartFormat.duration(3665.0))
    }

    @Test
    fun percent_rounds() {
        assertEquals("32%", ChartFormat.percent(0.316))
        assertEquals("0%", ChartFormat.percent(0.0))
        assertEquals("100%", ChartFormat.percent(1.0))
    }

    @Test
    fun distance_tick_matches_g_rule() {
        assertEquals("5", ChartFormat.distanceTick(5.0))
        assertEquals("2.5", ChartFormat.distanceTick(2.5))
        assertEquals("0.5", ChartFormat.distanceTick(0.5))
        assertEquals("12.25", ChartFormat.distanceTick(12.25))
        assertEquals("-1.5", ChartFormat.distanceTick(-1.5))
        assertEquals("nan", ChartFormat.distanceTick(Double.NaN))
    }

    @Test
    fun time_and_int_ticks() {
        assertEquals("", ChartFormat.timeTick(0.05))
        assertEquals("", ChartFormat.timeTick(0.1))
        assertEquals("15:00", ChartFormat.timeTick(15.0))
        assertEquals("178", ChartFormat.intTick(178.6))
    }

    @Test
    fun segment_count_policy_is_distance_proportional() {
        assertEquals(10, ChartConfig.segmentCountFor(10.5, XMode.DISTANCE))
        assertEquals(0, ChartConfig.segmentCountFor(0.8, XMode.DISTANCE))
        assertEquals(120, ChartConfig.segmentCountFor(500.0, XMode.DISTANCE))
        assertEquals(0, ChartConfig.segmentCountFor(10.5, XMode.TIME))
    }

    @Test
    fun heart_rate_helpers() {
        // D3: UNKNOWN = 여성 공식(낮은 쪽). 30세: 남 190, 여/불명 179(절삭).
        assertEquals(190, HeartRateZoneEngine.maxHeartRate(30, Gender.MALE))
        assertEquals(179, HeartRateZoneEngine.maxHeartRate(30, Gender.FEMALE))
        assertEquals(179, HeartRateZoneEngine.maxHeartRate(30, Gender.UNKNOWN))
        assertEquals(0, HeartRateZoneEngine.maxHeartRate(0, Gender.MALE)) // 생일 미입력
    }

    @Test
    fun donut_data_assembly_zero_to_null() {
        assertEquals(null, HeartRateZoneEngine.donutData(listOf(0.0, 0.0, 0.0, 0.0, 0.0), emptyList()))
        val data = HeartRateZoneEngine.donutData(listOf(10.0, 0.0, 20.0, 0.0, 5.0), listOf("웜업", "저강도"))
        assertEquals(5, data!!.segments.size)
        assertEquals("웜업", data.segments[0].label)
        assertEquals(null, data.segments[2].label)
    }

    @Suppress("DEPRECATION") // 구 API 동작 보존 검증 — 신 API는 SeriesSelectionTest
    @Test
    fun inverted_axes_follow_pace_slot() {
        assertEquals(emptySet(), SeriesSelection.invertedAxesFor(-1))
        assertEquals(setOf(com.lumipol.graph.model.Axis.PRIMARY), SeriesSelection.invertedAxesFor(0))
        assertEquals(setOf(com.lumipol.graph.model.Axis.SECONDARY), SeriesSelection.invertedAxesFor(1))
        assertEquals(setOf(com.lumipol.graph.model.Axis.SECONDARY), SeriesSelection.invertedAxesFor(2)) // AOS 슬롯2+ 누락 버그 소거
    }
}
