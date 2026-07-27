package com.lumipol.graph

import com.lumipol.graph.model.*
import kotlin.math.ceil

/** 최대심박 공식 입력 성별(C3). UNKNOWN 규칙은 D3 결정(44 문서). */
enum class Gender { MALE, FEMALE, UNKNOWN }

/**
 * 심박존 집계 — RDHeartRateZoneCalculator 이식.
 * 존 경계 비율(50/60/70/80/90%)로 심박 시계열을 Z1~5 누적시간으로 집계하고,
 * 같은 비율로 존별 bpm 경계를 산출해 도넛·범례 일치를 보장한다.
 * 최대심박 공식(C3)·도넛 조립(C5)도 코어 소유 — 앱 2벌의 상반 규칙(D3) 회수.
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

    /**
     * 나이 기반 최대심박(C3) — 남 Fox `220−age`, 여 Gulati `206−0.88×age`(양 앱 일치 공식,
     * Double 연산·소수 절삭·0 가드까지 동일). 나이 무효(0 이하 — 생일 미입력)면 0(존 카드 무데이터).
     * UNKNOWN은 D3 결정: 보수적으로 낮은 쪽(여성 공식) — 존5 과대평가 방지
     * (종전 iOS=여성/AOS=남성으로 정반대였던 발산의 단일 규칙).
     */
    fun maxHeartRate(age: Int, gender: Gender): Int {
        if (age <= 0) return 0
        val maxHR = when (gender) {
            Gender.MALE -> 220.0 - age
            Gender.FEMALE, Gender.UNKNOWN -> 206.0 - 0.88 * age
        }
        return if (maxHR > 0) maxHR.toInt() else 0
    }

    /**
     * 존별 누적 초 → 도넛 입력(C5). [zoneSeconds]는 [calculate] 결과(Z1~Z5 순),
     * [labels]는 존 표시명(부족하면 라벨 없음). **전 존 0이면 null** — "데이터 없음" 도넛을
     * 그릴지 말지는 앱 화면 정책이므로 조립 규칙(0→null)만 코어가 확정한다.
     */
    fun donutData(zoneSeconds: List<Double>, labels: List<String>): DonutChartData? {
        if (zoneSeconds.isEmpty() || zoneSeconds.all { it <= 0.0 }) return null
        val roles = listOf(
            DonutColorRole.ZONE1, DonutColorRole.ZONE2, DonutColorRole.ZONE3,
            DonutColorRole.ZONE4, DonutColorRole.ZONE5,
        )
        val segments = zoneSeconds.mapIndexed { i, seconds ->
            DonutSegment(seconds, roles.getOrElse(i) { DonutColorRole.ZONE5 }, labels.getOrNull(i))
        }
        return DonutChartData(segments)
    }
}
