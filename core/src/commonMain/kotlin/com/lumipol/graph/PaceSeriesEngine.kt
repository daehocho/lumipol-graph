package com.lumipol.graph

import com.lumipol.graph.model.*

/**
 * 페이스 시계열 전처리 — RDPaceChartDataBuilder.seriesPoints 이식.
 * 필터(하한 120s·상한 avg+600s) → 심박·케이던스·고도 결측 승계 → p95×1.25 아웃라이어 컷(최소 20표본)
 * → 다운샘플(skip=max(1,count/3000)) → best/valid 집계 → 고도 다운샘플 → 지표 가용성 확정.
 * x·paceSeconds는 앱이 단위·워치/GPS를 반영해 미리 계산해 넘긴다(코어는 도메인 프리).
 *
 * 결측은 입력의 null로만 판단한다([PaceSamplePoint]). 승계는 직전 유효값으로 채우되 앞쪽 결측은
 * 첫 유효값으로 소급하며, 세 지표(심박·케이던스·고도)가 같은 규칙이다. 가용성 판정은 다운샘플
 * **이전** 원본 기준이라 표본 수에 흔들리지 않고, 미가용 지표는 출력 필드도 함께 비운다.
 */
object PaceSeriesEngine {
    private const val PACE_MIN_SECONDS = 120.0
    private const val PACE_MAX_MARGIN_SECONDS = 600.0
    private const val SLOW_OUTLIER_PERCENTILE = 0.95
    private const val SLOW_OUTLIER_MARGIN = 1.25
    private const val SLOW_OUTLIER_MIN_SAMPLES = 20
    private const val DOWNSAMPLE_TARGET = 3000
    private const val METERS_PER_KM = 1000.0
    private const val SECONDS_PER_MINUTE = 60.0

    /** 페이스를 지표로 인정하는 최소 유효 표본 수(구 앱 규칙 "validPaceCount > 10"). */
    private const val MIN_VALID_PACE_COUNT = 11

    private data class S(val x: Double, val pace: Double, val hr: Double?, val cad: Double?, val alt: Double?)

    fun preprocess(input: PaceSeriesInput): PaceSeriesResult {
        if (input.sumDistanceMeters <= 0.0 || input.runningSeconds <= 0.0) {
            return PaceSeriesResult(emptyList(), emptyList(), emptyList(), null, 0.0, 0, emptySet())
        }
        val avg = input.runningSeconds / (input.sumDistanceMeters / METERS_PER_KM)
        val filterMax = avg + PACE_MAX_MARGIN_SECONDS
        val filterMin = PACE_MIN_SECONDS
        val skip = maxOf(1, input.points.size / DOWNSAMPLE_TARGET)

        val samples = ArrayList<S>(input.points.size)
        // 승계 시드: 앞쪽 결측을 첫 유효값으로 소급해 채운다. 세 지표 모두 같은 규칙이어야 한다 —
        // 한쪽만 소급하면 나머지는 결측 자리에 0이 남는데, 0은 이제 실측값이라 구멍과 구분되지 않는다.
        // 비용은 첫 유효값에서 끊기는 선형 탐색 3회(전부 결측인 입력에서만 full scan).
        var prevHr: Double? = input.points.firstOrNull { it.heartRate != null }?.heartRate
        var prevCad: Double? = input.points.firstOrNull { it.cadence != null }?.cadence
        var prevAlt: Double? = input.points.firstOrNull { it.altitude != null }?.altitude
        for (p in input.points) {
            var pace = p.paceSeconds
            if (pace <= 0.0 || pace < filterMin || pace >= filterMax) pace = 0.0
            val hr = p.heartRate ?: prevHr
            if (hr != null) prevHr = hr
            val cad = p.cadence ?: prevCad
            if (cad != null) prevCad = cad
            val alt = p.altitude ?: prevAlt
            if (alt != null) prevAlt = alt
            samples.add(S(p.x, pace, hr, cad, alt))
        }

        val slowCap = slowOutlierCap(samples.mapNotNull { if (it.pace > 0.0) it.pace else null })

        val pace = ArrayList<Point>()
        val heart = ArrayList<Point>()
        val cadence = ArrayList<Point>()
        var best = Double.MAX_VALUE
        var valid = 0
        for (s in samples) {
            // 승계 시드 덕분에 여기서 null인 건 "전 구간 결측"뿐이고, 그 시리즈는 아래에서 통째로 비운다.
            heart.add(Point(s.x, s.hr ?: 0.0))
            cadence.add(Point(s.x, s.cad ?: 0.0))
            if (s.pace <= 0.0 || s.pace > slowCap) continue
            best = minOf(best, s.pace)
            valid += 1
            if (valid % skip == 0) pace.add(Point(s.x, s.pace / SECONDS_PER_MINUTE))
        }
        val area = ArrayList<Point>()
        samples.forEachIndexed { i, s -> if (i % skip == 0) area.add(Point(s.x, s.alt ?: 0.0)) }

        // 가용성 판정 → 미가용 지표는 필드도 비운다. 네 지표가 같은 규약이라 앱이 `availableSeries`를
        // 안 보고 필드만 읽어도 어긋나지 않는다(한쪽만 남으면 "코어는 없다는데 그려지는" 틈이 생긴다).
        // 판정은 다운샘플 이전 원본(any) 기준이라 표본 수에 흔들리지 않고, 다운샘플로 리스트가
        // 실제로 비는 경우(isNotEmpty)만 추가로 배제한다.
        // 평지 컷은 두지 않는다 — "그릴지"(가용성)와 "얼마나 크게 그릴지"(정규화 하한)는 다른 축이고,
        // 후자는 렌더의 minSpan이 맡는다. 여기서 자르면 평지가 미측정과 구분되지 않는다.
        val hasPace = valid >= MIN_VALID_PACE_COUNT && pace.isNotEmpty()
        val hasHeart = input.points.any { it.heartRate != null }
        val hasCadence = input.points.any { it.cadence != null }
        val hasAltitude = input.points.any { it.altitude != null } && area.size >= 2

        val paceOut = if (hasPace) pace else emptyList()
        val heartOut = if (hasHeart) heart else emptyList()
        val cadenceOut = if (hasCadence) cadence else emptyList()
        val areaOut = if (hasAltitude) area else null
        val bestOut = if (best == Double.MAX_VALUE) 0.0 else best

        val available = buildSet {
            if (hasPace) add(PaceSeriesId.PACE)
            if (hasHeart) add(PaceSeriesId.HEART)
            if (hasCadence) add(PaceSeriesId.CADENCE)
            if (hasAltitude) add(PaceSeriesId.ALTITUDE)
        }

        return PaceSeriesResult(
            pace = paceOut, heart = heartOut, cadence = cadenceOut,
            altitudeArea = areaOut,
            bestPaceSeconds = bestOut, validPaceCount = valid,
            availableSeries = available,
        )
    }

    private fun slowOutlierCap(validPaceSeconds: List<Double>): Double {
        if (validPaceSeconds.size < SLOW_OUTLIER_MIN_SAMPLES) return Double.MAX_VALUE
        val sorted = validPaceSeconds.sorted()
        val index = (SLOW_OUTLIER_PERCENTILE * (sorted.size - 1)).toInt()
        return sorted[index] * SLOW_OUTLIER_MARGIN
    }
}
