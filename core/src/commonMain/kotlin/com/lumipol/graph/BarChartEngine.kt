package com.lumipol.graph

import com.lumipol.graph.model.*
import com.lumipol.graph.scale.AxisDomain
import com.lumipol.graph.scale.Y_AXIS_HEADROOM_FRACTION
import com.lumipol.graph.scale.niceScale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 스플릿 막대 집계·레이아웃 엔진.
 * - samples를 splitDistanceMeters마다 끊어 스플릿 페이스(시간가중, sec/unit) 산정.
 * - 마지막 잔여는 부분 스플릿(isPartial).
 * - colorRole: targetPace(없으면 전체 평균) ± tolerance 밴드로 FASTER/ON_TARGET/SLOWER.
 * - 축 반전: 페이스는 낮을수록 빠르므로 heightFraction·position을 1-normalize로 뒤집는다
 *   — 맨 위 틱이 가장 빠른 페이스, 빠른 스플릿일수록 막대가 길다.
 */
object BarChartEngine {

    private data class RawBar(
        val value: Double,
        val isPartial: Boolean,
        val endMinutes: Int?,
        val endDistanceMeters: Double? = null,
        val endSeconds: Double? = null,
    )

    // 시간모드 버킷 선택 정책(양 플랫폼 공유). 총 시간으로 막대가 MAX_BARS 이하가 되는 최소 후보(분).
    private val BUCKET_MINUTE_CANDIDATES = listOf(1, 2, 5, 10)
    private const val MAX_BARS = 10

    /** 막대 하한 — 짧은 런에서 막대 1~2개만 보이던 문제(0.41.0). 두 모드 공유. */
    private const val MIN_BARS = 5

    /** 거리모드 버킷 후보(페이스 단위의 분수). km 기준 1000·500·200·100·50m. */
    private val DISTANCE_BUCKET_FRACTIONS = listOf(1.0, 0.5, 0.2, 0.1, 0.05)

    /**
     * 총거리로 스플릿 버킷 거리(m)를 고른다. 큰 후보부터 내려가며 막대가 [MIN_BARS] 이상이 되는
     * 첫 후보를 쓰고, 끝까지 못 채우면 하한(unit/20)을 쓴다. 반환값은 **버킷일 뿐**이고
     * 페이스 단위(sec/unit)는 [unitMeters]가 계속 맡는다.
     */
    fun chooseDistanceBucketMeters(totalDistanceMeters: Double, unitMeters: Double): Double {
        require(unitMeters > 0) { "unitMeters must be > 0" }
        if (!(totalDistanceMeters > 0.0) || totalDistanceMeters.isInfinite()) return unitMeters
        for (f in DISTANCE_BUCKET_FRACTIONS) {
            val bucket = unitMeters * f
            if (ceil(totalDistanceMeters / bucket) >= MIN_BARS) return bucket
        }
        return unitMeters * DISTANCE_BUCKET_FRACTIONS.last()
    }

    /** 시간모드 하향 후보(초) — 기존 분 후보로 [MIN_BARS]를 못 채울 때만 쓴다. */
    private val SHORT_BUCKET_SECOND_CANDIDATES = listOf(30.0, 15.0)

    /**
     * 총 러닝 시간(초)으로 시간 버킷 크기(초)를 고른다.
     * 1) 기존 규칙(iOS bucketMinutes 동일) — 막대가 [MAX_BARS] 이하가 되는 최소 분 후보.
     * 2) 그 결과가 [MIN_BARS] 미만일 때만 30초·15초로 내려간다 — 5분 이상 런의 선택은 불변(0.41.0).
     */
    fun chooseTimeBucketSeconds(runningSeconds: Double): Double {
        val totalMinutes = runningSeconds / 60.0
        var chosen = BUCKET_MINUTE_CANDIDATES.last() * 60.0
        for (n in BUCKET_MINUTE_CANDIDATES) {
            if (ceil(totalMinutes / n).toInt() <= MAX_BARS) { chosen = n * 60.0; break }
        }
        if (barCount(runningSeconds, chosen) >= MIN_BARS) return chosen
        for (c in SHORT_BUCKET_SECOND_CANDIDATES) {
            if (barCount(runningSeconds, c) >= MIN_BARS) return c
        }
        return SHORT_BUCKET_SECOND_CANDIDATES.last()
    }

    private fun barCount(runningSeconds: Double, bucketSeconds: Double): Int =
        if (!(runningSeconds > 0.0) || runningSeconds.isInfinite()) 0
        else ceil(runningSeconds / bucketSeconds).toInt()

    fun layout(data: BarChartData): BarChartLayout {
        val paceUnit = data.splitDistanceMeters
        require(paceUnit > 0) { "splitDistanceMeters must be > 0" }

        var totalDist = 0.0
        var totalTime = 0.0
        val raw = if (data.splitTimeSeconds != null) {
            aggregateByTime(data, paceUnit) { d, t -> totalDist += d; totalTime += t }
        } else {
            val bucket = chooseDistanceBucketMeters(validDistanceSum(data.samples), paceUnit)
            aggregateByDistance(data, bucket, paceUnit) { d, t -> totalDist += d; totalTime += t }
        }

        if (raw.isEmpty()) return BarChartLayout(emptyList(), emptyList(), null, null)

        // 색 기준(ref): 명시 목표 → 런 총합 평균 → 필터 샘플 합 평균.
        val ref = data.targetPaceSecPerUnit
            ?: runTotalsRef(data, paceUnit)
            ?: (totalTime / (totalDist / paceUnit))
        val tol = data.toleranceSecPerUnit

        val ys = raw.map { it.value } + ref
        val ns = niceScale(ys.min(), ys.max(), data.maxTicks, Y_AXIS_HEADROOM_FRACTION)
        val dom = AxisDomain(ns.niceMin, ns.niceMax)

        val bars = raw.mapIndexed { idx, b ->
            val role = when {
                b.value < ref - tol -> BarColorRole.FASTER
                b.value > ref + tol -> BarColorRole.SLOWER
                else -> BarColorRole.ON_TARGET
            }
            BarLayout(
                idx, b.value, 1.0 - dom.normalize(b.value), role, b.isPartial,
                b.endMinutes, b.endDistanceMeters, b.endSeconds,
            )
        }
        val yTicks = ns.ticks.map { AxisTick(it, 1.0 - dom.normalize(it)) }
        return BarChartLayout(bars, yTicks, 1.0 - dom.normalize(ref), colorAnchors(raw, runTotalsRef(data, paceUnit)))
    }

    /**
     * 컬러맵 색 앵커 — 렌더러·앱 4벌 복제를 대체하는 단일 원본(BarColorAnchors KDoc 참조).
     * 부분 스플릿은 표본 시간이 짧아 극값을 왜곡하므로 온전 스플릿이 범위를 이루면 배제한다.
     */
    private fun colorAnchors(raw: List<RawBar>, runAverage: Double?): BarColorAnchors {
        val full = raw.filter { !it.isPartial }.map { it.value }
        val hasRange = full.size >= 2 && full.max() > full.min()
        val anchor = if (hasRange) full else raw.map { it.value }
        val fastest = anchor.min()
        val slowest = anchor.max()
        // 런 총합 평균이 스플릿 극값 밖이면 색 구간이 붕괴하므로 클램프(소비 앱 실사고 대응 규칙 흡수)
        val average = (runAverage?.takeIf { it > 0.0 } ?: (anchor.sum() / anchor.size))
            .coerceIn(fastest, slowest)
        return BarColorAnchors(fastest, slowest, average)
    }

    // 런 총합 기반 색 기준(총거리>0일 때만). iOS 시간모드가 넘기던 runningTime/(sumDistance/unit).
    private fun runTotalsRef(data: BarChartData, unit: Double): Double? {
        val dur = data.totalDurationSeconds ?: return null
        val dist = data.totalDistanceMeters ?: return null
        return if (dist > 0.0) dur / (dist / unit) else null
    }

    // 거리 버킷 집계(endMinutes=null). bucket은 끊는 간격, paceUnit은 값 정규화 단위(sec/unit) —
    // 짧은 런에서 bucket만 작아지고 페이스 표기는 km/mile 기준으로 남는다(0.41.0).
    private inline fun aggregateByDistance(
        data: BarChartData, bucket: Double, paceUnit: Double, onValid: (Double, Double) -> Unit,
    ): List<RawBar> {
        val raw = mutableListOf<RawBar>()
        var accDist = 0.0
        var accTime = 0.0
        var covered = 0.0 // 막대로 확정된 누적 거리(m)
        for (s in data.samples) {
            val d = s.distanceMeters; val t = s.timeSeconds
            if (d <= 0.0 || t <= 0.0 || d.isNaN() || t.isNaN() || d.isInfinite() || t.isInfinite()) continue
            accDist += d; accTime += t; onValid(d, t)
            while (accDist >= bucket) {
                val overflow = accDist - bucket
                val overflowTime = if (d > 0.0) overflow * (t / d) else 0.0
                val barTime = accTime - overflowTime
                covered += bucket
                raw.add(RawBar(
                    barTime / (bucket / paceUnit), isPartial = false, endMinutes = null,
                    endDistanceMeters = covered,
                ))
                accDist = overflow; accTime = overflowTime
            }
        }
        if (accDist > 0.0 && accTime > 0.0) {
            covered += accDist
            raw.add(RawBar(
                accTime / (accDist / paceUnit), isPartial = true, endMinutes = null,
                endDistanceMeters = covered,
            ))
        }
        return raw
    }

    /** 버킷 선택용 총거리 — 집계와 같은 유효성 규칙(거리>0·시간>0·유한)으로 미리 합산한다. */
    private fun validDistanceSum(samples: List<SplitSample>): Double {
        var sum = 0.0
        for (s in samples) {
            val d = s.distanceMeters; val t = s.timeSeconds
            if (d <= 0.0 || t <= 0.0 || d.isNaN() || t.isNaN() || d.isInfinite() || t.isInfinite()) continue
            sum += d
        }
        return sum
    }

    // 시간 버킷 집계. 버킷 경계에서 오버플로를 나누지 않고(현행 iOS와 동일) 통째 flush.
    // endMinutes = max(1, round(누적경과초/60)) — 누적 경과는 버킷 간 리셋하지 않는다.
    private fun aggregateByTime(
        data: BarChartData, paceUnit: Double, onValid: (Double, Double) -> Unit,
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
                raw.add(RawBar(
                    accTime / (accDist / paceUnit), isPartial = false,
                    endMinutes = endMin(), endSeconds = elapsed,
                ))
                accDist = 0.0; accTime = 0.0
            }
        }
        if (accDist > 0.0 && accTime > 0.0) {
            raw.add(RawBar(
                accTime / (accDist / paceUnit), isPartial = true,
                endMinutes = endMin(), endSeconds = elapsed,
            ))
        }
        return raw
    }
}
