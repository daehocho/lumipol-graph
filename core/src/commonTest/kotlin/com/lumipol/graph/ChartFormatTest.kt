package com.lumipol.graph

import com.lumipol.graph.model.BarChartLayout
import com.lumipol.graph.model.BarColorRole
import com.lumipol.graph.model.BarLayout
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
    fun split_end_distance_rounds_to_two_decimals() {
        // 부분 스플릿 끝은 임의 값(0.83844 등) — distanceTick(6자리)이 아닌 2자리 반올림.
        assertEquals("0.84", ChartFormat.splitEndDistance(0.83844))
        assertEquals("0.89", ChartFormat.splitEndDistance(0.89))
        assertEquals("0.2", ChartFormat.splitEndDistance(0.2))
        assertEquals("0.9", ChartFormat.splitEndDistance(0.895))
        assertEquals("1", ChartFormat.splitEndDistance(1.0))
        assertEquals("42.2", ChartFormat.splitEndDistance(42.195))
        assertEquals("nan", ChartFormat.splitEndDistance(Double.NaN))
        assertEquals("inf", ChartFormat.splitEndDistance(Double.POSITIVE_INFINITY))
    }

    @Test
    fun split_end_time_uses_unpadded_minutes() {
        // 시간모드 마지막 부분 버킷의 실제 끝 시각 — timeTick(9:00)과 같은 무패딩 분 표기.
        // duration(MM:SS 패딩)이면 09:21이 되어 옆 온전 막대(9:00)와 표기가 갈린다.
        assertEquals("9:21", ChartFormat.splitEndTime(561.0))
        assertEquals("9:00", ChartFormat.splitEndTime(540.0))
        assertEquals("0:29", ChartFormat.splitEndTime(29.9))
        assertEquals("1:01:05", ChartFormat.splitEndTime(3665.0))
        assertEquals("0:00", ChartFormat.splitEndTime(0.0))
        assertEquals("0:00", ChartFormat.splitEndTime(Double.NaN))
    }

    @Test
    fun time_and_int_ticks() {
        assertEquals("", ChartFormat.timeTick(0.05))
        assertEquals("", ChartFormat.timeTick(0.1))
        assertEquals("15:00", ChartFormat.timeTick(15.0))
        assertEquals("178", ChartFormat.intTick(178.6))
    }

    private fun timeBar(i: Int, sec: Double, min: Int, partial: Boolean = false) = BarLayout(
        i, 300.0, 0.5, BarColorRole.ON_TARGET, partial, endMinutes = min, endSeconds = sec,
    )

    private fun barLayoutOf(vararg bars: BarLayout) =
        BarChartLayout(bars.toList(), emptyList(), null, null)

    @Test
    fun split_x_axis_labels_time_mode_branches() {
        // 분 단위 버킷 — 온전=timeTick(9:00), 마지막 부분=splitEndTime(19:01)
        val normal = barLayoutOf(timeBar(0, 600.0, 10), timeBar(1, 1141.0, 19, partial = true))
        assertEquals(listOf("10:00", "19:01"), ChartFormat.splitXAxisLabels(normal, 1000.0))
        // sub-minute 버킷(첫 끝 < 60초) — 전 라벨 duration(mm:ss)
        val sub = barLayoutOf(timeBar(0, 30.0, 1), timeBar(1, 55.0, 1, partial = true))
        assertEquals(listOf("00:30", "00:55"), ChartFormat.splitXAxisLabels(sub, 1000.0))
        // 1시간 초과(마지막 끝 > 3600) — 전 라벨 timeTickHour로 통일(0.47.0 규칙)
        val hour = barLayoutOf(timeBar(0, 600.0, 10), timeBar(5, 3600.0, 60), timeBar(6, 3665.0, 61, partial = true))
        assertEquals(listOf("0:10:00", "1:00:00", "1:01:05"), ChartFormat.splitXAxisLabels(hour, 1000.0))
    }

    @Test
    fun split_x_axis_labels_hour_gate_is_strict_at_exactly_3600() {
        // 게이트는 엄격 초과(> 3600) — 정확히 1시간에 끝난 런은 분 표기를 유지한다. `>=`로 바뀌면
        // 전 라벨이 60:00 → 1:00:00으로 뒤집히는데 종전 테스트(마지막 3665)는 그걸 못 잡았다.
        val exact = barLayoutOf(timeBar(0, 1800.0, 30), timeBar(1, 3600.0, 60))
        assertEquals(listOf("30:00", "60:00"), ChartFormat.splitXAxisLabels(exact, 1000.0))
        // 1초만 넘으면 전 라벨 h:mm:ss.
        val over = barLayoutOf(timeBar(0, 1800.0, 30), timeBar(1, 3601.0, 60))
        assertEquals(listOf("0:30:00", "1:00:01"), ChartFormat.splitXAxisLabels(over, 1000.0))
    }

    @Test
    fun split_x_axis_labels_accepts_injected_crosses_hour_trigger() {
        // 0.49.0: 축 끝(유효 델타 합)이 1시간 이하라도 총 운동 시간이 넘으면 h:mm:ss로 통일한다 —
        // 부분 버킷이 없는 분할에서는 총 시간 스냅이 걸리지 않아 축 끝만으로는 판정할 수 없고,
        // 같은 화면 라인차트(timeTick(minutes, crossesHour))와 표기가 갈렸다.
        val noPartial = barLayoutOf(timeBar(0, 600.0, 10), timeBar(1, 3600.0, 60))
        assertEquals(
            listOf("10:00", "60:00"),
            ChartFormat.splitXAxisLabels(noPartial, 1000.0, crossesHour = false),
        )
        assertEquals(
            listOf("0:10:00", "1:00:00"),
            ChartFormat.splitXAxisLabels(noPartial, 1000.0, crossesHour = true),
        )
        // 2인자판은 crossesHour=false와 동일(기존 호출부 무회귀).
        assertEquals(
            ChartFormat.splitXAxisLabels(noPartial, 1000.0),
            ChartFormat.splitXAxisLabels(noPartial, 1000.0, crossesHour = false),
        )
        // OR 규칙: 주입이 false여도 축 끝이 넘으면 통일한다 — 안 그러면 부분 버킷
        // splitEndTime(1:01:05, 시)과 온전 timeTick(61:00, 분)이 한 축에 섞인다.
        val overRun = barLayoutOf(timeBar(0, 3660.0, 61), timeBar(1, 3665.0, 61, partial = true))
        assertEquals(
            listOf("1:01:00", "1:01:05"),
            ChartFormat.splitXAxisLabels(overRun, 1000.0, crossesHour = false),
        )
        // 거리모드는 트리거와 무관.
        val dist = barLayoutOf(
            BarLayout(0, 300.0, 0.5, BarColorRole.ON_TARGET, false, endDistanceMeters = 500.0),
        )
        assertEquals(listOf("0.5"), ChartFormat.splitXAxisLabels(dist, 1000.0, crossesHour = true))
    }

    @Test
    fun split_x_axis_labels_distance_and_legacy_fallback() {
        fun distBar(i: Int, meters: Double?) = BarLayout(
            i, 300.0, 0.5, BarColorRole.ON_TARGET, false, endDistanceMeters = meters,
        )
        val d = barLayoutOf(distBar(0, 500.0), distBar(1, 890.0))
        assertEquals(listOf("0.5", "0.89"), ChartFormat.splitXAxisLabels(d, 1000.0))
        // 끝 필드가 둘 다 없는 레거시 layout — index+1 폴백(양 앱 기존 폴백과 동일)
        val legacy = barLayoutOf(distBar(0, null), distBar(1, null))
        assertEquals(listOf("1", "2"), ChartFormat.splitXAxisLabels(legacy, 1000.0))
    }

    @Test
    fun time_tick_axis_context_overload() {
        // crossesHour=false는 기존 timeTick 그대로, true면 전 눈금 h:mm:ss(원점 생략 유지)
        assertEquals("50:00", ChartFormat.timeTick(50.0, crossesHour = false))
        assertEquals("0:50:00", ChartFormat.timeTick(50.0, crossesHour = true))
        assertEquals("3:20:00", ChartFormat.timeTick(200.0, crossesHour = true))
        assertEquals("", ChartFormat.timeTick(0.1, crossesHour = true))
        assertEquals("", ChartFormat.timeTick(Double.NaN, crossesHour = true))
    }

    @Test
    fun time_tick_hour_unifies_axis_notation() {
        // 1시간 넘는 축의 전 라벨 통일 표기(0.47.0) — 시 무패딩, 분·초 패딩.
        // 60:00(분)과 1:01:05(시)가 한 축에 섞이지 않게 전부 h:mm:ss로.
        assertEquals("0:10:00", ChartFormat.timeTickHour(600.0))
        assertEquals("1:00:00", ChartFormat.timeTickHour(3600.0))
        assertEquals("1:01:05", ChartFormat.timeTickHour(3665.0))
        assertEquals("0:00:29", ChartFormat.timeTickHour(29.9))
        assertEquals("0:00:00", ChartFormat.timeTickHour(0.0))
        assertEquals("0:00:00", ChartFormat.timeTickHour(Double.NaN))
        assertEquals("0:00:00", ChartFormat.timeTickHour(Double.POSITIVE_INFINITY))
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
