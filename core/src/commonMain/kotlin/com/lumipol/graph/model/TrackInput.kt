package com.lumipol.graph.model

/**
 * 거리 단위 — 마일 환산 상수의 **단일 원본**(C1). 종전 양 앱에 3벌씩 병존하던 상수
 * (1.609344000000865 / 0.621371 / 1609.344 — 곱이 1이 아닌 역수 불일치)를 정리한다:
 * [METERS_PER_MILE]만 정의하고 나머지는 전부 파생.
 */
enum class DistanceUnit {
    KILOMETERS, MILES;

    /** 이 단위 1칸의 미터 수 — 페이스(초/단위)·스플릿 폭 계산의 기준. */
    val unitMeters: Double
        get() = when (this) {
            KILOMETERS -> METERS_PER_KM
            MILES -> METERS_PER_MILE
        }

    companion object {
        const val METERS_PER_KM: Double = 1000.0
        const val METERS_PER_MILE: Double = 1609.344
    }
}

/** X축 모드 — 거리(단위 누적) 또는 시간(분). */
enum class XMode { DISTANCE, TIME }

/**
 * 원천 → 차트 입력 빌드 옵션(C1).
 * @property useWatchSpeed 워치 운동 기록(D2: 워치 speed 우선 경로). iOS `Track.useRundayWatch`.
 */
data class BuildOptions(
    val unit: DistanceUnit,
    val xMode: XMode,
    val useWatchSpeed: Boolean,
) {
    // ObjC 기본 인자 소실 대응.
    constructor(unit: DistanceUnit, xMode: XMode) : this(unit, xMode, false)
}

/** 런 1회 총합 — 전처리 상한 avg·시간모드 색 기준 계산용. */
data class RunTotals(val sumDistanceMeters: Double, val runningSeconds: Double)

/**
 * 앱의 DB 행을 그대로 옮긴 원천 레코드(C1) — **계산·해석 없음**, 결측은 null.
 * 누적/델타 중 앱 저장소가 가진 쪽만 채우면 된다(코어가 상호 보완 재구성):
 * AOS는 누적(realDistance·realExerciseTime), iOS는 델타(distance·timeInterval) + 누적(exerciseTime).
 *
 * 센티널 해석(심박·케이던스 0 = 결측, 고도 ≤ −100 = 미측정, 케이던스 상한 250)은 양 앱 동일
 * 규칙이라 [sanitized] 팩토리가 흡수한다 — 앱은 DB 원본값을 그대로 넘긴다.
 */
data class RawTrackSample(
    val cumulativeDistanceMeters: Double?,
    val deltaDistanceMeters: Double?,
    val cumulativeSeconds: Double?,
    val deltaSeconds: Double?,
    val speedMps: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val heartRate: Double?,
    val cadence: Double?,
    val altitude: Double?,
) {
    companion object {
        /** 미측정 고도 센티널 — iOS `> -100.f` 관행·AOS `INVALID_ALTITUDE` 동일 값. */
        const val INVALID_ALTITUDE: Double = -100.0

        /** 케이던스 상한 — COROS 등 한쪽 발 2배 저장 이상치 방어(양 앱 동일 값). */
        const val MAX_CADENCE: Double = 250.0

        /** DB 원본값 → 샘플. 센티널 해석·이상치 클램프를 이 한 곳에 모은다(C1). */
        fun sanitized(
            cumulativeDistanceMeters: Double?,
            deltaDistanceMeters: Double?,
            cumulativeSeconds: Double?,
            deltaSeconds: Double?,
            speedMps: Double?,
            latitude: Double?,
            longitude: Double?,
            rawHeartRate: Double?,
            rawCadence: Double?,
            rawAltitude: Double?,
        ): RawTrackSample = RawTrackSample(
            cumulativeDistanceMeters = cumulativeDistanceMeters,
            deltaDistanceMeters = deltaDistanceMeters,
            cumulativeSeconds = cumulativeSeconds,
            deltaSeconds = deltaSeconds,
            speedMps = speedMps,
            latitude = latitude,
            longitude = longitude,
            heartRate = rawHeartRate?.takeIf { it > 0.0 },
            cadence = rawCadence?.takeIf { it > 0.0 }?.coerceAtMost(MAX_CADENCE),
            altitude = rawAltitude?.takeIf { it > INVALID_ALTITUDE },
        )
    }
}
