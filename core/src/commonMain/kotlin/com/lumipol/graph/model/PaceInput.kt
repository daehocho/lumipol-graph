package com.lumipol.graph.model

/**
 * 전처리 입력 1포인트 — 앱이 x·paceSeconds를 미리 계산(단위·워치/GPS 반영).
 *
 * **결측은 null로 넘긴다.** 센티널(0, -100 등)이나 "값이 전부 같으면 미측정" 같은 추론에
 * 의존하지 않는다 — 앱마다 센티널이 달라 지표별 가용성 판정이 갈리던 원인이었다.
 * 결측 승계(직전 유효값으로 채우기)는 코어가 수행하므로 앱이 미리 채우지 않는다.
 */
data class PaceSamplePoint(
    val x: Double,             // 앱 계산 x(거리 km/mile 또는 시간 분)
    val paceSeconds: Double,   // 앱 계산 페이스(초/단위). 필터 전 raw — 0 이하는 물리적 무효라 필터가 처리
    val heartRate: Double?,    // bpm, null = 미측정
    val cadence: Double?,      // spm, null = 결측
    val altitude: Double?,     // m, null = 미측정
)

/** Track 1회 전처리 입력. */
data class PaceSeriesInput(
    val points: List<PaceSamplePoint>,
    val runningSeconds: Double,      // 상한 avg 계산용 런 총 시간(s)
    val sumDistanceMeters: Double,   // 상한 avg 계산용 런 총 거리(m)
)
