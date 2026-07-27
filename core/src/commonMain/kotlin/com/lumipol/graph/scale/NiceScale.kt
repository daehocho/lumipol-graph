package com.lumipol.graph.scale

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

data class NiceScale(
    val niceMin: Double,
    val niceMax: Double,
    val step: Double,
    val ticks: List<Double>,
)

/** 라인·막대 차트 Y축 공용 헤드룸 비율 — 축이 데이터 min/max에 딱 붙지 않게 하는 기본값. */
const val Y_AXIS_HEADROOM_FRACTION = 0.05

/**
 * Heckbert "nice numbers" 알고리즘.
 * [headroomFraction] > 0이면 계산 전에 min/max를 범위의 해당 비율씩 바깥으로 밀어
 * 축이 데이터에 딱 붙지 않게 한다(원래 min ≥ 0이면 0 아래로는 내리지 않는다 —
 * 심박·케이던스 등 음수 불가 지표 보호). X축처럼 데이터 끝에 맞춰야 하는 축은 기본값 0.0.
 */
fun niceScale(min: Double, max: Double, maxTicks: Int = 5, headroomFraction: Double = 0.0): NiceScale {
    require(maxTicks >= 2) { "maxTicks must be >= 2" }
    require(headroomFraction >= 0.0) { "headroomFraction must be >= 0" }
    if (min == max) {
        val pad = if (min == 0.0) 1.0 else abs(min) * 0.5
        return niceScale(min - pad, min + pad, maxTicks, headroomFraction)
    }
    val lo0 = minOf(min, max)
    val hi0 = maxOf(min, max)
    val headroom = (hi0 - lo0) * headroomFraction
    val lo = (lo0 - headroom).let { if (lo0 >= 0.0) maxOf(it, 0.0) else it }
    val hi = hi0 + headroom
    val range = niceNum(hi - lo, round = false)
    val step = niceNum(range / (maxTicks - 1), round = true)
    val niceMin = floor(lo / step) * step
    val niceMax = ceil(hi / step) * step
    val ticks = buildList {
        var t = niceMin
        while (t <= niceMax + step * 0.5) {
            add(t)
            t += step
        }
    }
    return NiceScale(niceMin, niceMax, step, ticks)
}

// round=false 는 <= 로, round=true 는 < 로 구간을 나누는 비대칭 경계는 실수가 아니라
// Heckbert의 표준 "nice numbers" 공식 그대로다(범위 산정 시엔 보수적으로 올림, 스텝 산정 시엔 반올림).
private fun niceNum(x: Double, round: Boolean): Double {
    val exp = floor(log10(x))
    val f = x / 10.0.pow(exp)
    val nf = if (round) {
        when {
            f < 1.5 -> 1.0
            f < 3.0 -> 2.0
            f < 7.0 -> 5.0
            else -> 10.0
        }
    } else {
        when {
            f <= 1.0 -> 1.0
            f <= 2.0 -> 2.0
            f <= 5.0 -> 5.0
            else -> 10.0
        }
    }
    return nf * 10.0.pow(exp)
}
