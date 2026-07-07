package com.lumipol.graph.model

/** 도넛 세그먼트 색 역할 — 코어는 역할만, 실제 색은 렌더러가 주입. 심박존 Z1~5. */
enum class DonutColorRole { ZONE1, ZONE2, ZONE3, ZONE4, ZONE5 }

/** 도넛 한 조각의 입력값. value는 누적 시간(초) 등 임의 양수 크기. */
data class DonutSegment(val value: Double, val colorRole: DonutColorRole)

/** 도넛 차트 입력 — 세그먼트 목록만. */
data class DonutChartData(val segments: List<DonutSegment>)
