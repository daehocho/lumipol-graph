package com.lumipol.graph.model

/** 도넛 세그먼트 색 역할 — 코어는 역할만, 실제 색은 렌더러가 주입. 심박존 Z1~5. */
enum class DonutColorRole { ZONE1, ZONE2, ZONE3, ZONE4, ZONE5 }

/** 도넛 한 조각의 입력값. value는 누적 시간(초) 등 임의 양수 크기.
 *  [label]은 센터 라벨용 표시 이름(예: "무산소") — null이면 렌더러가 퍼센트만 표시. */
data class DonutSegment(
    val value: Double,
    val colorRole: DonutColorRole,
    val label: String? = null,
) {
    // ObjC export는 기본 인자를 내보내지 않는다 — 기존 Swift 호출부(value:colorRole:) 보존용.
    constructor(value: Double, colorRole: DonutColorRole) : this(value, colorRole, null)
}

/** 도넛 차트 입력 — 세그먼트 목록만. */
data class DonutChartData(val segments: List<DonutSegment>)
