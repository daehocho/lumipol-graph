package com.lumipol.graph.model

/** 원시에 가까운 세그먼트: 이전 포인트와의 거리 델타(m), 시간 델타(s). */
data class SplitSample(val distanceMeters: Double, val timeSeconds: Double)

/** 막대 색 역할 — 코어는 역할만, 실제 색은 앱이 주입. */
enum class BarColorRole { FASTER, ON_TARGET, SLOWER }

/**
 * 스플릿 막대 입력.
 * @param splitDistanceMeters 스플릿 1칸 거리(km=1000.0, mile=1609.344). 앱이 사용자 단위로 결정.
 * @param targetPaceSecPerUnit 목표 페이스(sec/unit). null이면 러닝 전체 평균을 기준으로 색 판정.
 * @param toleranceSecPerUnit onTarget 판정 ± 밴드(초).
 */
data class BarChartData(
    val samples: List<SplitSample>,
    val splitDistanceMeters: Double,
    val targetPaceSecPerUnit: Double? = null,
    val toleranceSecPerUnit: Double = 10.0,
    val maxTicks: Int = 5,
)
