package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.math.ceil

/**
 * 심박존 집계 — RDHeartRateZoneCalculator 이식.
 * 존 경계 비율(50/60/70/80/90%)로 심박 시계열을 Z1~5 누적시간으로 집계하고,
 * 같은 비율로 존별 bpm 경계를 산출해 도넛·범례 일치를 보장한다.
 * 최대심박 산출(나이·성별)은 앱 도메인이라 코어 밖.
 */
object HeartRateZoneEngine {
    // 존 하한 비율. index 0=Z1(50~60%) … 4=Z5(≥90%). 50% 미만은 어느 존에도 제외.
    private val ZONE_LOWER_FRACTIONS = listOf(0.50, 0.60, 0.70, 0.80, 0.90)

    /** 심박 시계열 → 존별 누적 초(size = 존 수). maxHeartRate<=0이면 전부 0. */
    fun calculate(samples: List<HeartRateZoneSample>, maxHeartRate: Int): List<Double> {
        val empty = List(ZONE_LOWER_FRACTIONS.size) { 0.0 }
        if (maxHeartRate <= 0) return empty
        val maxHR = maxHeartRate.toDouble()
        val acc = DoubleArray(ZONE_LOWER_FRACTIONS.size)
        for (s in samples) {
            if (s.heartRate <= 0.0 || s.timeInterval <= 0.0) continue
            val frac = s.heartRate / maxHR
            if (frac < ZONE_LOWER_FRACTIONS[0]) continue
            var zone = 0
            for (i in ZONE_LOWER_FRACTIONS.indices.reversed()) {
                if (frac >= ZONE_LOWER_FRACTIONS[i]) { zone = i; break }
            }
            acc[zone] += s.timeInterval
        }
        return acc.toList()
    }

    /**
     * 각 존의 표시용 bpm 경계. index 0=Z1 … 4=Z5. upper=null이면 최대존.
     * calculate와 동일 경계(하한 포함)여야 라벨·집계가 일치. 존 하한 bpm = ceil(비율×maxHR).
     */
    fun zoneBpmRanges(maxHeartRate: Int): List<ZoneBpmRange> {
        if (maxHeartRate <= 0) return emptyList()
        val maxHR = maxHeartRate.toDouble()
        val lower = ZONE_LOWER_FRACTIONS.map { ceil(maxHR * it).toInt() }
        return listOf(
            ZoneBpmRange(lower[0], lower[1] - 1),
            ZoneBpmRange(lower[1], lower[2] - 1),
            ZoneBpmRange(lower[2], lower[3] - 1),
            ZoneBpmRange(lower[3], lower[4] - 1),
            ZoneBpmRange(lower[4], null),
        )
    }
}
