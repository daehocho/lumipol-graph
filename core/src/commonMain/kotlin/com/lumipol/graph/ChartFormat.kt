package com.lumipol.graph

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

    /** 시간 축 tick(분) → `N:00`. 0.1 이하는 빈 문자열(원점 라벨 생략 — 양 앱 동일). */
    fun timeTick(minutes: Double): String =
        if (minutes.isNaN() || minutes.isInfinite() || minutes <= 0.1) "" else "${minutes.toInt()}:00"

    /** 정수 축 tick — 절삭(양 앱 `value.toInt()` 동일). */
    fun intTick(value: Double): String =
        if (value.isNaN() || value.isInfinite()) "0" else "${value.toInt()}"

    private fun pad2(n: Int): String = if (n < 10) "0$n" else "$n"
}
