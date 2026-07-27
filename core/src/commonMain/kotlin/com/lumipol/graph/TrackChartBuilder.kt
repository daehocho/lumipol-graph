package com.lumipol.graph

import com.lumipol.graph.model.BuildOptions
import com.lumipol.graph.model.HeartRateZoneSample
import com.lumipol.graph.model.PaceSamplePoint
import com.lumipol.graph.model.PaceSeriesInput
import com.lumipol.graph.model.RawTrackSample
import com.lumipol.graph.model.RunTotals
import com.lumipol.graph.model.SplitSample
import com.lumipol.graph.model.XMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * 원천 샘플 → 차트 입력 전처리(C1) — 페이스 계산식·무효 게이트·x 산출·스플릿 델타·HR존 dt를
 * 코어가 단독 소유한다. 종전 양 앱이 서로 다르게 구현하던 최상류 발산 지점(11 문서 §1)의 회수.
 *
 * 2점 페이스 계산식은 D2 결정(44 문서)의 병합안:
 * - 워치 기록([BuildOptions.useWatchSpeed])이면 워치 speed 우선(iOS 경로)
 * - GPS 경로는 거리 델타, 누적 델타 ≤ 0이면 위경도 Haversine 폴백(AOS 경로 — 단 AOS의
 *   ×1000 이중 환산 결함은 소거하고 미터로 올바르게 계산)
 * - 1~41km/h 속도 게이트(AOS 이상치 방어)를 워치·GPS 공통 적용
 * - Double 정밀도, floorToDecimal 내림 없음(코어 필터가 방어)
 */
object TrackChartBuilder {

    /** D2 게이트 — 이 밖의 속도는 무효 페이스(0) 처리. AOS 1~41km/h(exclusive) 채택. */
    private const val MIN_SPEED_KMH = 1.0
    private const val MAX_SPEED_KMH = 41.0

    /**
     * 원천 → 라인 차트 전처리 입력. 필터·결측 승계·다운샘플·가용성 판정은
     * [PaceSeriesEngine.preprocess]가 이어받는다(앱은 이 결과를 그대로 엔진에 넘긴다).
     * 무효 총합(거리·시간 ≤ 0)이면 빈 포인트 입력을 반환한다(iOS guard 동일).
     */
    fun paceInput(samples: List<RawTrackSample>, totals: RunTotals, options: BuildOptions): PaceSeriesInput {
        if (totals.sumDistanceMeters <= 0.0 || totals.runningSeconds <= 0.0) {
            return PaceSeriesInput(emptyList(), totals.runningSeconds, totals.sumDistanceMeters)
        }
        val unitMeters = options.unit.unitMeters
        var cumMeters = 0.0
        var cumSeconds = 0.0
        var prev: RawTrackSample? = null
        val points = ArrayList<PaceSamplePoint>(samples.size)
        for (s in samples) {
            val deltaMeters = deltaMeters(prev, s)
            val deltaSeconds = deltaSeconds(prev, s)
            // 누적값: 저장소가 준 누적을 신뢰, 없으면 델타 합산(iOS abs 규칙 유지).
            cumMeters = s.cumulativeDistanceMeters ?: (cumMeters + abs(deltaMeters ?: 0.0))
            cumSeconds = s.cumulativeSeconds ?: (cumSeconds + (deltaSeconds ?: 0.0))
            val x = when (options.xMode) {
                XMode.DISTANCE -> cumMeters / unitMeters
                XMode.TIME -> cumSeconds / 60.0
            }
            val paceSeconds = paceSecondsPerUnit(
                watch = options.useWatchSpeed,
                speedMps = s.speedMps,
                prev = prev,
                cur = s,
                deltaMeters = deltaMeters,
                deltaSeconds = deltaSeconds,
                unitMeters = unitMeters,
            )
            points.add(
                PaceSamplePoint(
                    x = x,
                    paceSeconds = paceSeconds,
                    heartRate = s.heartRate,
                    cadence = s.cadence,
                    altitude = s.altitude,
                ),
            )
            prev = s
        }
        return PaceSeriesInput(points, totals.runningSeconds, totals.sumDistanceMeters)
    }

    /**
     * 원천 → 스플릿 델타. 무효 델타(거리·시간 ≤ 0)는 제외(양 앱 동일 규칙).
     * 누적만 있는 저장소(AOS)는 첫 행의 누적값이 시작 구간이다(prev=0 시드 — AOS 빌더 동일).
     */
    fun splitSamples(samples: List<RawTrackSample>): List<SplitSample> {
        var prev: RawTrackSample? = null
        val out = ArrayList<SplitSample>(samples.size)
        for (s in samples) {
            val d = deltaMeters(prev, s) ?: 0.0
            val t = deltaSeconds(prev, s) ?: 0.0
            if (d > 0.0 && t > 0.0) out.add(SplitSample(distanceMeters = d, timeSeconds = t))
            prev = s
        }
        return out
    }

    /**
     * 원천 → HR존 집계 입력. dt 재구성 규칙은 D11 결정(iOS 폴백 채택):
     * - 누적 운동시간이 존재하면(마지막 행 누적 > 0) 누적 델타 — 일시정지 제외, 역전은 0으로 클램프
     * - 누적이 전부 비어 있으면(2021.01 데이터 정합 이전 기록) per-point 시간 델타로 폴백 —
     *   폴백 없이는 구 기록의 심박존이 통째로 무데이터가 된다
     */
    fun zoneSamples(samples: List<RawTrackSample>): List<HeartRateZoneSample> {
        val hasCumulativeTime = (samples.lastOrNull()?.cumulativeSeconds ?: 0.0) > 0.0
        var prevSeconds = 0.0
        return samples.map { s ->
            val dt: Double
            if (hasCumulativeTime) {
                val elapsed = s.cumulativeSeconds ?: 0.0
                dt = maxOf(0.0, elapsed - prevSeconds)
                prevSeconds = maxOf(prevSeconds, elapsed)
            } else {
                dt = maxOf(0.0, s.deltaSeconds ?: 0.0)
            }
            HeartRateZoneSample(heartRate = s.heartRate ?: 0.0, timeInterval = dt)
        }
    }

    // ── 내부 규칙 ────────────────────────────────────────────

    /** 거리 델타(m): 저장소 델타 우선, 없으면 누적 차. 첫 행은 누적값 자체(0 시드). */
    private fun deltaMeters(prev: RawTrackSample?, cur: RawTrackSample): Double? {
        cur.deltaDistanceMeters?.let { return it }
        val curCum = cur.cumulativeDistanceMeters ?: return null
        val prevCum = prev?.cumulativeDistanceMeters ?: 0.0
        return curCum - prevCum
    }

    /** 시간 델타(s): 저장소 델타 우선, 없으면 누적 차. */
    private fun deltaSeconds(prev: RawTrackSample?, cur: RawTrackSample): Double? {
        cur.deltaSeconds?.let { return it }
        val curCum = cur.cumulativeSeconds ?: return null
        val prevCum = prev?.cumulativeSeconds ?: 0.0
        return curCum - prevCum
    }

    /** D2 병합 페이스(초/단위). 무효면 0(코어 필터가 걸러낸다). */
    private fun paceSecondsPerUnit(
        watch: Boolean,
        speedMps: Double?,
        prev: RawTrackSample?,
        cur: RawTrackSample,
        deltaMeters: Double?,
        deltaSeconds: Double?,
        unitMeters: Double,
    ): Double {
        val effectiveSpeedMps: Double = if (watch) {
            speedMps?.takeIf { it > 0.0 && !it.isNaN() && !it.isInfinite() } ?: return 0.0
        } else {
            var d = deltaMeters ?: return 0.0
            val t = deltaSeconds ?: return 0.0
            if (d <= 0.0) {
                // D2 폴백: 누적 델타 ≤ 0이면 위경도 직접 거리(m). 좌표 없으면 무효.
                d = haversineMeters(prev, cur) ?: return 0.0
            }
            if (d <= 0.0 || t <= 0.0) return 0.0
            d / t
        }
        // D2 게이트(1~41km/h) — 워치·GPS 공통 이상치 방어.
        val speedKmh = effectiveSpeedMps * 3.6
        if (speedKmh <= MIN_SPEED_KMH || speedKmh >= MAX_SPEED_KMH) return 0.0
        return unitMeters / effectiveSpeedMps
    }

    /** 지구 반경(m) — WGS84 평균. */
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * 위경도 2점 Haversine 거리(m). 좌표가 없으면 null.
     *
     * libm 주의(경계 정책 §5): sin/cos가 필요하다 — GPS 폴백 근사 거리로만 쓰이고 결과는
     * 1e-6m로 양자화해 JVM/Native trig ULP 차이가 비트에 남지 않게 한다(골든이 실측 감시).
     */
    private fun haversineMeters(prev: RawTrackSample?, cur: RawTrackSample): Double? {
        val lat1 = prev?.latitude ?: return null
        val lon1 = prev.longitude ?: return null
        val lat2 = cur.latitude ?: return null
        val lon2 = cur.longitude ?: return null
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val rLat1 = lat1 * PI / 180.0
        val rLat2 = lat2 * PI / 180.0
        // boundary-allow: Haversine 폴백 — 결과를 1e-6m 양자화해 결정론 확보(위 KDoc)
        val a = sinHalfSq(dLat) + kotlin.math.cos(rLat1) * kotlin.math.cos(rLat2) * sinHalfSq(dLon)
        val meters = 2.0 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return floor(meters * 1e6 + 0.5) / 1e6
    }

    // boundary-allow: Haversine 폴백 전용 sin — 위 KDoc의 양자화 규칙 참조
    private fun sinHalfSq(x: Double): Double = kotlin.math.sin(x / 2.0).let { it * it }
}
