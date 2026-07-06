package com.lumipol.graph

import com.lumipol.graph.model.*
import com.lumipol.graph.scale.AxisDomain
import com.lumipol.graph.scale.niceScale

/**
 * 스플릿 막대 집계·레이아웃 엔진.
 * - samples를 splitDistanceMeters마다 끊어 스플릿 페이스(시간가중, sec/unit) 산정.
 * - 마지막 잔여는 부분 스플릿(isPartial).
 * - colorRole: targetPace(없으면 전체 평균) ± tolerance 밴드로 FASTER/ON_TARGET/SLOWER.
 * - value는 크기 그대로 정규화(heightFraction) — 페이스 방향(낮을수록 빠름)은 색과 y틱으로 전달.
 */
object BarChartEngine {

    private data class RawBar(val value: Double, val isPartial: Boolean)

    fun layout(data: BarChartData): BarChartLayout {
        val unit = data.splitDistanceMeters
        require(unit > 0) { "splitDistanceMeters must be > 0" }

        val raw = mutableListOf<RawBar>()
        var accDist = 0.0
        var accTime = 0.0
        var totalDist = 0.0
        var totalTime = 0.0

        for (s in data.samples) {
            val d = s.distanceMeters
            val t = s.timeSeconds
            if (d <= 0.0 || t <= 0.0 || d.isNaN() || t.isNaN() || d.isInfinite() || t.isInfinite()) continue
            accDist += d; accTime += t
            totalDist += d; totalTime += t
            while (accDist >= unit) {
                // 경계를 넘은 오버플로는 현재 샘플에 속하므로 그 rate(t/d)로 시간을 뗀다
                val overflow = accDist - unit
                val overflowTime = if (d > 0.0) overflow * (t / d) else 0.0
                val barTime = accTime - overflowTime  // unit 구간에 해당하는 시간
                // 정확히 unit 거리를 덮으므로 value(sec/unit) = barTime
                raw.add(RawBar(barTime, isPartial = false))
                accDist = overflow
                accTime = overflowTime
            }
        }
        if (accDist > 0.0 && accTime > 0.0) {
            raw.add(RawBar(accTime / (accDist / unit), isPartial = true))
        }

        if (raw.isEmpty()) {
            return BarChartLayout(emptyList(), emptyList(), null)
        }

        val ref = data.targetPaceSecPerUnit ?: (totalTime / (totalDist / unit))
        val tol = data.toleranceSecPerUnit

        val ys = raw.map { it.value } + ref
        val ns = niceScale(ys.min(), ys.max(), data.maxTicks)
        val dom = AxisDomain(ns.niceMin, ns.niceMax)

        val bars = raw.mapIndexed { idx, b ->
            val role = when {
                b.value < ref - tol -> BarColorRole.FASTER
                b.value > ref + tol -> BarColorRole.SLOWER
                else -> BarColorRole.ON_TARGET
            }
            BarLayout(idx, b.value, dom.normalize(b.value), role, b.isPartial)
        }
        val yTicks = ns.ticks.map { AxisTick(it, dom.normalize(it)) }
        return BarChartLayout(bars, yTicks, dom.normalize(ref))
    }
}
