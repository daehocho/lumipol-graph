package com.lumipol.graph.model

/** 심박존 집계 입력 1샘플. */
data class HeartRateZoneSample(
    val heartRate: Double,    // bpm, 0 이하 = 미측정
    val timeInterval: Double, // 이전 포인트와의 시간 간격(s)
)
