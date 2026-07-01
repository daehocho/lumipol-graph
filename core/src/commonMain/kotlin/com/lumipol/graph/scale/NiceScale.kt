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

/** Heckbert "nice numbers" 알고리즘. */
fun niceScale(min: Double, max: Double, maxTicks: Int = 5): NiceScale {
    require(maxTicks >= 2) { "maxTicks must be >= 2" }
    if (min == max) {
        val pad = if (min == 0.0) 1.0 else abs(min) * 0.5
        return niceScale(min - pad, min + pad, maxTicks)
    }
    val lo = minOf(min, max)
    val hi = maxOf(min, max)
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
