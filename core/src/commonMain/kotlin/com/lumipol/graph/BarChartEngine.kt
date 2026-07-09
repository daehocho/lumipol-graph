package com.lumipol.graph

import com.lumipol.graph.model.*
import com.lumipol.graph.scale.AxisDomain
import com.lumipol.graph.scale.niceScale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 스플릿 막대 집계·레이아웃 엔진.
 * - samples를 splitDistanceMeters마다 끊어 스플릿 페이스(시간가중, sec/unit) 산정.
 * - 마지막 잔여는 부분 스플릿(isPartial).
 * - colorRole: targetPace(없으면 전체 평균) ± tolerance 밴드로 FASTER/ON_TARGET/SLOWER.
 * - value는 크기 그대로 정규화(heightFraction) — 페이스 방향(낮을수록 빠름)은 색과 y틱으로 전달.
 */
object BarChartEngine {

    private data class RawBar(val value: Double, val isPartial: Boolean, val endMinutes: Int?)

    // 시간모드 버킷 선택 정책(양 플랫폼 공유). 총 시간으로 막대가 MAX_BARS 이하가 되는 최소 후보(분).
    private val BUCKET_MINUTE_CANDIDATES = listOf(1, 2, 5, 10)
    private const val MAX_BARS = 10

    /** 총 러닝 시간(초)으로 시간 버킷 크기(초)를 고른다. iOS bucketMinutes 규칙과 동일. */
    fun chooseTimeBucketSeconds(runningSeconds: Double): Double {
        val totalMinutes = runningSeconds / 60.0
        for (n in BUCKET_MINUTE_CANDIDATES) {
            val bars = ceil(totalMinutes / n).toInt()
            if (bars <= MAX_BARS) return n * 60.0
        }
        return BUCKET_MINUTE_CANDIDATES.last() * 60.0
    }

    fun layout(data: BarChartData): BarChartLayout {
        val unit = data.splitDistanceMeters
        require(unit > 0) { "splitDistanceMeters must be > 0" }

        var totalDist = 0.0
        var totalTime = 0.0
        val raw = if (data.splitTimeSeconds != null) {
            aggregateByTime(data, unit) { d, t -> totalDist += d; totalTime += t }
        } else {
            aggregateByDistance(data, unit) { d, t -> totalDist += d; totalTime += t }
        }

        if (raw.isEmpty()) return BarChartLayout(emptyList(), emptyList(), null)

        // 색 기준(ref): 명시 목표 → 런 총합 평균 → 필터 샘플 합 평균.
        val ref = data.targetPaceSecPerUnit
            ?: runTotalsRef(data, unit)
            ?: (totalTime / (totalDist / unit))
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
            BarLayout(idx, b.value, dom.normalize(b.value), role, b.isPartial, b.endMinutes)
        }
        val yTicks = ns.ticks.map { AxisTick(it, dom.normalize(it)) }
        return BarChartLayout(bars, yTicks, dom.normalize(ref))
    }

    // 런 총합 기반 색 기준(총거리>0일 때만). iOS 시간모드가 넘기던 runningTime/(sumDistance/unit).
    private fun runTotalsRef(data: BarChartData, unit: Double): Double? {
        val dur = data.totalDurationSeconds ?: return null
        val dist = data.totalDistanceMeters ?: return null
        return if (dist > 0.0) dur / (dist / unit) else null
    }

    // 거리 버킷 집계(기존 로직, endMinutes=null).
    private inline fun aggregateByDistance(
        data: BarChartData, unit: Double, onValid: (Double, Double) -> Unit,
    ): List<RawBar> {
        val raw = mutableListOf<RawBar>()
        var accDist = 0.0
        var accTime = 0.0
        for (s in data.samples) {
            val d = s.distanceMeters; val t = s.timeSeconds
            if (d <= 0.0 || t <= 0.0 || d.isNaN() || t.isNaN() || d.isInfinite() || t.isInfinite()) continue
            accDist += d; accTime += t; onValid(d, t)
            while (accDist >= unit) {
                val overflow = accDist - unit
                val overflowTime = if (d > 0.0) overflow * (t / d) else 0.0
                val barTime = accTime - overflowTime
                raw.add(RawBar(barTime, isPartial = false, endMinutes = null))
                accDist = overflow; accTime = overflowTime
            }
        }
        if (accDist > 0.0 && accTime > 0.0) {
            raw.add(RawBar(accTime / (accDist / unit), isPartial = true, endMinutes = null))
        }
        return raw
    }

    // 시간 버킷 집계. 버킷 경계에서 오버플로를 나누지 않고(현행 iOS와 동일) 통째 flush.
    // endMinutes = max(1, round(누적경과초/60)) — 누적 경과는 버킷 간 리셋하지 않는다.
    private fun aggregateByTime(
        data: BarChartData, unit: Double, onValid: (Double, Double) -> Unit,
    ): List<RawBar> {
        val bucket = data.splitTimeSeconds!!
        val raw = mutableListOf<RawBar>()
        var accDist = 0.0
        var accTime = 0.0
        var elapsed = 0.0
        fun endMin() = maxOf(1, (elapsed / 60.0).roundToInt())
        for (s in data.samples) {
            val d = s.distanceMeters; val t = s.timeSeconds
            if (d <= 0.0 || t <= 0.0 || d.isNaN() || t.isNaN() || d.isInfinite() || t.isInfinite()) continue
            accDist += d; accTime += t; elapsed += t; onValid(d, t)
            if (accTime >= bucket) {
                raw.add(RawBar(accTime / (accDist / unit), isPartial = false, endMinutes = endMin()))
                accDist = 0.0; accTime = 0.0
            }
        }
        if (accDist > 0.0 && accTime > 0.0) {
            raw.add(RawBar(accTime / (accDist / unit), isPartial = true, endMinutes = endMin()))
        }
        return raw
    }
}
