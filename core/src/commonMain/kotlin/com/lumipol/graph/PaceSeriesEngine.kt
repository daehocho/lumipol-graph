package com.lumipol.graph

import com.lumipol.graph.model.*

/**
 * 페이스 시계열 전처리 — RDPaceChartDataBuilder.seriesPoints 이식.
 * 필터(하한 120s·상한 avg+600s) → 심박·케이던스 결측 승계 → p95×1.25 아웃라이어 컷(최소 20표본)
 * → 다운샘플(skip=max(1,count/3000)) → best/valid 집계 → 고도 다운샘플+평지판정(0.5m).
 * x·paceSeconds는 앱이 단위·워치/GPS를 반영해 미리 계산해 넘긴다(코어는 도메인 프리).
 */
object PaceSeriesEngine {
    private const val PACE_MIN_SECONDS = 120.0
    private const val PACE_MAX_MARGIN_SECONDS = 600.0
    private const val SLOW_OUTLIER_PERCENTILE = 0.95
    private const val SLOW_OUTLIER_MARGIN = 1.25
    private const val SLOW_OUTLIER_MIN_SAMPLES = 20
    private const val DOWNSAMPLE_TARGET = 3000
    private const val FLAT_ALTITUDE_THRESHOLD = 0.5
    private const val METERS_PER_KM = 1000.0
    private const val SECONDS_PER_MINUTE = 60.0

    private data class S(val x: Double, val pace: Double, val hr: Double, val cad: Double, val alt: Double)

    fun preprocess(input: PaceSeriesInput): PaceSeriesResult {
        if (input.sumDistanceMeters <= 0.0 || input.runningSeconds <= 0.0) {
            return PaceSeriesResult(emptyList(), emptyList(), emptyList(), null, 0.0, 0)
        }
        val avg = input.runningSeconds / (input.sumDistanceMeters / METERS_PER_KM)
        val filterMax = avg + PACE_MAX_MARGIN_SECONDS
        val filterMin = PACE_MIN_SECONDS
        val skip = maxOf(1, input.points.size / DOWNSAMPLE_TARGET)

        val samples = ArrayList<S>(input.points.size)
        var prevHr = 0.0
        var prevCad = 0.0
        for (p in input.points) {
            var pace = p.paceSeconds
            if (pace <= 0.0 || pace < filterMin || pace >= filterMax) pace = 0.0
            val hr = if (p.heartRate <= 0.0 && prevHr > 0.0) prevHr else p.heartRate
            if (hr > 0.0) prevHr = hr
            val cad = if (p.cadence <= 0.0 && prevCad > 0.0) prevCad else p.cadence
            if (cad > 0.0) prevCad = cad
            samples.add(S(p.x, pace, hr, cad, p.altitude))
        }

        val slowCap = slowOutlierCap(samples.mapNotNull { if (it.pace > 0.0) it.pace else null })

        val pace = ArrayList<Point>()
        val heart = ArrayList<Point>()
        val cadence = ArrayList<Point>()
        var best = Double.MAX_VALUE
        var valid = 0
        for (s in samples) {
            heart.add(Point(s.x, s.hr))
            cadence.add(Point(s.x, s.cad))
            if (s.pace <= 0.0 || s.pace > slowCap) continue
            best = minOf(best, s.pace)
            valid += 1
            if (valid % skip == 0) pace.add(Point(s.x, s.pace / SECONDS_PER_MINUTE))
        }
        val heartOut = if (heart.all { it.y <= 0.0 }) emptyList() else heart
        val cadenceOut = if (cadence.all { it.y <= 0.0 }) emptyList() else cadence
        val bestOut = if (best == Double.MAX_VALUE) 0.0 else best

        val area = ArrayList<Point>()
        samples.forEachIndexed { i, s -> if (i % skip == 0) area.add(Point(s.x, s.alt)) }
        val ys = area.map { it.y }
        val hasElevation = area.size >= 2 && ((ys.max() - ys.min()) > FLAT_ALTITUDE_THRESHOLD)

        return PaceSeriesResult(
            pace = pace, heart = heartOut, cadence = cadenceOut,
            altitudeArea = if (hasElevation) area else null,
            bestPaceSeconds = bestOut, validPaceCount = valid,
        )
    }

    private fun slowOutlierCap(validPaceSeconds: List<Double>): Double {
        if (validPaceSeconds.size < SLOW_OUTLIER_MIN_SAMPLES) return Double.MAX_VALUE
        val sorted = validPaceSeconds.sorted()
        val index = (SLOW_OUTLIER_PERCENTILE * (sorted.size - 1)).toInt()
        return sorted[index] * SLOW_OUTLIER_MARGIN
    }
}
