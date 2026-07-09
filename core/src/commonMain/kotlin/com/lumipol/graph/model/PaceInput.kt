package com.lumipol.graph.model

/** 전처리 입력 1포인트 — 앱이 x·paceSeconds를 미리 계산(단위·워치/GPS 반영). */
data class PaceSamplePoint(
    val x: Double,            // 앱 계산 x(거리 km/mile 또는 시간 분)
    val paceSeconds: Double,  // 앱 계산 페이스(초/단위). 필터 전 raw(무효면 0 이하)
    val heartRate: Double,    // bpm, 0 이하 = 미측정
    val cadence: Double,      // spm, 0 이하 = 결측
    val altitude: Double,     // m
)

/** Track 1회 전처리 입력. */
data class PaceSeriesInput(
    val points: List<PaceSamplePoint>,
    val runningSeconds: Double,      // 상한 avg 계산용 런 총 시간(s)
    val sumDistanceMeters: Double,   // 상한 avg 계산용 런 총 거리(m)
)
