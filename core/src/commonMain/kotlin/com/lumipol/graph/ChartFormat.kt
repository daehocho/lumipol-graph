package com.lumipol.graph

import com.lumipol.graph.model.BarChartLayout
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 차트 표시 문자열 규칙(C2) — ko-KR 고정(05 승인), 로케일 API 미사용.
 * 렌더러·앱의 포맷 발산(`4'30"` vs `5'30''`, 사문 폴백 등 — 11 문서 §2)의 단일 원본.
 * 페이스 표기는 D1 결정(44 문서): iOS 표기(`분'초"`) + 99분 상한.
 */
object ChartFormat {

    /** 유효 페이스 하한(초/단위) — iOS PACE_MIN(0.1분). 이하면 물리적 무효로 간주. */
    const val PACE_MIN_SECONDS: Double = 6.0

    /** 유효 페이스 상한(초/단위) — D1: 99분 상한(도달 시 무효 표기). */
    const val PACE_MAX_SECONDS: Double = 99.0 * 60.0

    /** 페이스(초/단위) → `4'30"`. 무효(0·NaN·Inf·하한 미만·99분 이상)는 [paceInvalid]. */
    fun pace(seconds: Double): String {
        if (seconds.isNaN() || seconds.isInfinite()) return paceInvalid()
        if (seconds < PACE_MIN_SECONDS || seconds >= PACE_MAX_SECONDS) return paceInvalid()
        val min = (seconds / 60.0).toInt()
        val sec = (seconds - min * 60.0).toInt()
        return "$min'${pad2(sec)}\""
    }

    /** 무효 페이스 표기 — D1: iOS `-'--"` 채택(AOS `-'--''` 사문 폴백 자연 해소). */
    fun paceInvalid(): String = "-'--\""

    /** 시간(초) → `H:MM:SS` / `MM:SS`. 0 이하·NaN·Inf는 `00:00`(양 앱 동일 규칙). */
    fun duration(seconds: Double): String {
        if (seconds <= 0.0 || seconds.isNaN() || seconds.isInfinite()) return "00:00"
        val total = seconds.toInt()
        val hour = total / 3600
        val min = (total - hour * 3600) / 60
        val sec = total % 60
        return if (hour > 0) "$hour:${pad2(min)}:${pad2(sec)}" else "${pad2(min)}:${pad2(sec)}"
    }

    /** 비율(0~1) → `N%`(반올림) — 도넛 센터·범례 공용. */
    fun percent(fraction: Double): String = "${(fraction * 100).roundToInt()}%"

    /**
     * 거리 축 tick — `%g` 동등 규칙: 정수는 소수점 없이, 그 외엔 트레일링 0 제거.
     * nice tick 값(1·2·2.5·5 × 10^n) 전제 — 소수 6자리 안에서 정확히 떨어지는 값만 온전 보장,
     * 그 밖은 소수 6자리 반올림 후 0 제거(로케일·libm 비의존 결정론 구현).
     */
    fun distanceTick(value: Double): String {
        if (value.isNaN()) return "nan"
        if (value.isInfinite()) return if (value > 0) "inf" else "-inf"
        if (value == floor(value) && abs(value) < 1e15) return value.toLong().toString()
        val negative = value < 0
        val v = abs(value)
        // 소수 6자리 고정 반올림 → 정수부.소수부 조립 → 트레일링 0 제거.
        val scaled = floor(v * 1e6 + 0.5).toLong()
        val intPart = scaled / 1_000_000
        val fracPart = (scaled % 1_000_000).toString().padStart(6, '0').trimEnd('0')
        val body = if (fracPart.isEmpty()) intPart.toString() else "$intPart.$fracPart"
        return if (negative) "-$body" else body
    }

    /**
     * 스플릿 막대 끝 누적 거리 라벨 → 소수 2자리 반올림 후 [distanceTick] 표기 규칙
     * (정수는 소수점 없이, 트레일링 0 제거). [distanceTick]은 nice tick 값 전제라 부분
     * 스플릿의 임의 값(0.83844 등)이 6자리로 새어 나온다 — 막대 끝 라벨은 이 함수를 쓴다.
     */
    fun splitEndDistance(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return distanceTick(value)
        val negative = value < 0
        val scaled = floor(abs(value) * 100 + 0.5).toLong()
        val intPart = scaled / 100
        val fracPart = (scaled % 100).toString().padStart(2, '0').trimEnd('0')
        val body = if (fracPart.isEmpty()) intPart.toString() else "$intPart.$fracPart"
        return if (negative) "-$body" else body
    }

    /**
     * 스플릿 막대 끝 누적 시간(초) → 무패딩 분 `m:ss`(1시간 이상은 `h:mm:ss`).
     * 시간모드 마지막 부분 버킷용 — `endMinutes` 반올림이면 잔여 30초 미만이 직전 온전
     * 막대와 같은 라벨(9:00, 9:00)로 뭉개진다. 온전 막대의 [timeTick](`9:00`)과 표기 정합.
     */
    fun splitEndTime(seconds: Double): String {
        if (seconds <= 0.0 || seconds.isNaN() || seconds.isInfinite()) return "0:00"
        val total = seconds.toInt()
        val hour = total / 3600
        val min = (total - hour * 3600) / 60
        val sec = total % 60
        return if (hour > 0) "$hour:${pad2(min)}:${pad2(sec)}" else "$min:${pad2(sec)}"
    }

    /** 시간 축 tick(분) → `N:00`. 0.1 이하는 빈 문자열(원점 라벨 생략 — 양 앱 동일). */
    fun timeTick(minutes: Double): String =
        if (minutes.isNaN() || minutes.isInfinite() || minutes <= 0.1) "" else "${minutes.toInt()}:00"

    /**
     * 시간모드 x축 라벨(초) → 항상 `h:mm:ss`(`0:10:00`, `1:00:00`, `1:01:05`).
     * 축의 마지막 막대 끝이 1시간을 넘으면(endSeconds > 3600) **전 라벨**을 이 표기로 통일한다
     * — 온전 버킷 [timeTick](`60:00`)과 부분 버킷 [splitEndTime](`1:01:05`)이 한 축에서 두
     * 표기 체계로 섞이는 문제의 해소(0.47.0 결정). 1시간 이하 축은 기존 표기를 그대로 쓴다.
     * 트리거 판정(마지막 endSeconds > 3600)은 호출자(앱) 몫. 무효(≤0·NaN·Inf)는 `0:00:00`.
     */
    fun timeTickHour(seconds: Double): String {
        if (seconds <= 0.0 || seconds.isNaN() || seconds.isInfinite()) return "0:00:00"
        val total = seconds.toInt()
        return "${total / 3600}:${pad2((total % 3600) / 60)}:${pad2(total % 60)}"
    }

    /**
     * [timeTick]의 축 단위 컨텍스트판 — 라인차트 시간모드 x축용. [crossesHour]면 전 눈금을
     * [timeTickHour]로 통일하고(0.47.0 규칙), 원점 생략(0.1분 이하 빈 문자열)은 유지한다.
     * 트리거는 **총 운동 시간 > 3600초** — 눈금 값이 아니라 축 단위로 판정해야 한 축에 두
     * 표기가 섞이지 않는다. 입력은 분.
     *
     * 같은 화면의 스플릿 카드와 표기를 맞추려면 **같은 값**을 [splitXAxisLabels]의
     * `crossesHour`에도 넘겨야 한다 — 스플릿 쪽 축 끝(`endSeconds`)은 유효 델타 합이라
     * 총 운동 시간과 다를 수 있고(부분 버킷이 없으면 총 시간 스냅이 걸리지 않는다),
     * 각자 판정하면 한 화면에서 `1:00:00`과 `60:00`이 동시에 찍힌다(0.49.0).
     */
    fun timeTick(minutes: Double, crossesHour: Boolean): String = when {
        !crossesHour -> timeTick(minutes)
        minutes.isNaN() || minutes.isInfinite() || minutes <= 0.1 -> ""
        else -> timeTickHour(minutes * 60.0)
    }

    /**
     * 스플릿 막대 x축 라벨 일괄 생성 — 양 앱이 각자 들고 있던 분기 관용구의 단일 원본(0.48.0).
     * 모드는 막대 필드로 판별한다(시간모드=endSeconds, 거리모드=endDistanceMeters, 둘 다 없는
     * 레거시 layout은 `index+1` 폴백). [unitMeters]는 거리모드 표시 단위(km=1000/mi=1609.344).
     *
     * 시간모드 표기(우선순위 순):
     * 1. 축이 1시간을 넘으면(마지막 endSeconds > 3600) 전 라벨 [timeTickHour] — `0:10:00 … 1:01:05`.
     * 2. sub-minute 버킷(첫 endSeconds < 60)은 [duration] — endMinutes가 1,1,2,2로 뭉개진다.
     * 3. 부분 버킷은 [splitEndTime](`9:21`), 온전 버킷은 [timeTick]의 endMinutes(`9:00`).
     * 거리모드는 [splitEndDistance]\(endDistanceMeters/unitMeters).
     *
     * 축 끝만으로 판정하는 이 오버로드는 같은 화면 라인차트와 갈릴 수 있다 — 트리거를 쥔
     * 호출자는 3인자판을 쓴다(0.49.0).
     */
    fun splitXAxisLabels(layout: BarChartLayout, unitMeters: Double): List<String> =
        splitXAxisLabels(layout, unitMeters, crossesHour = false)

    /**
     * [splitXAxisLabels]의 트리거 주입판 — 축 밖 원천(총 운동 시간)으로 1시간 초과를 아는
     * 호출자용(0.49.0). 라인차트 [timeTick]에 넘기는 `crossesHour`를 **그대로** 넘기면 한 화면의
     * 두 카드가 같은 표기 체계를 쓴다.
     *
     * 판정은 `crossesHour || 마지막 endSeconds > 3600`(OR)이다 — 주입 트리거가 false여도 축 끝이
     * 1시간을 넘으면 h:mm:ss로 통일한다. 축 끝 판정을 살려 두는 이유: 그 경우 부분 버킷
     * [splitEndTime]이 `1:01:05`(시), 온전 버킷 [timeTick]이 `61:00`(분)을 내 **한 축 안에서**
     * 두 표기가 섞인다(0.47.0이 없앤 문제). 즉 2인자판은 이 함수의 `crossesHour = false`와 같다.
     *
     * 트리거 원천 계약: 총 운동 시간(`BarChartData.totalDurationSeconds`와 같은 값) > 3600초.
     * 막대의 `endSeconds`는 **유효 델타 합**이라 일시정지 기록에서 총 시간보다 작고, 총 시간
     * 스냅([BarChartEngine] 시간 집계)은 마지막 **부분** 버킷에만 걸리므로 잔여 0으로 딱 나누어
     * 떨어지는 분할에서는 축 끝이 총 시간에 못 미친다.
     */
    fun splitXAxisLabels(layout: BarChartLayout, unitMeters: Double, crossesHour: Boolean): List<String> {
        require(unitMeters > 0) { "unitMeters must be > 0" }
        val bars = layout.bars
        val subMinuteBucket = (bars.firstOrNull()?.endSeconds ?: 60.0) < 60.0
        val hourAxis = crossesHour || (bars.lastOrNull()?.endSeconds ?: 0.0) > 3600.0
        return bars.map { bar ->
            val seconds = bar.endSeconds
            when {
                seconds != null -> when {
                    hourAxis -> timeTickHour(seconds)
                    subMinuteBucket -> duration(seconds)
                    bar.isPartial -> splitEndTime(seconds)
                    else -> timeTick((bar.endMinutes ?: (bar.index + 1)).toDouble())
                }
                else -> bar.endDistanceMeters?.let { splitEndDistance(it / unitMeters) }
                    ?: "${bar.index + 1}"
            }
        }
    }

    /** 정수 축 tick — 절삭(양 앱 `value.toInt()` 동일). */
    fun intTick(value: Double): String =
        if (value.isNaN() || value.isInfinite()) "0" else "${value.toInt()}"

    private fun pad2(n: Int): String = if (n < 10) "0$n" else "$n"
}
