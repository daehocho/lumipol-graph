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
    // 시간모드(신규): non-null이면 시간 버킷 집계. splitDistanceMeters는 페이스 단위로 유지.
    val splitTimeSeconds: Double? = null,
    // 시간모드 색 기준(런 평균) 계산용 런 총합. 둘 다 존재+총거리>0이면 ref로 사용.
    val totalDurationSeconds: Double? = null,
    val totalDistanceMeters: Double? = null,
) {
    // ObjC export는 기본 인자를 내보내지 않아 iOS 호출부가 8개 인자를 전부 하드코딩해야 했다
    // (브릿지 감사 §3 — 코어 기본값 변경이 iOS에 전파되지 않는 사고 지점). 축약 생성자 제공.
    constructor(samples: List<SplitSample>, splitDistanceMeters: Double) :
        this(samples, splitDistanceMeters, null, 10.0, 5, null, null, null)

    constructor(
        samples: List<SplitSample>,
        splitDistanceMeters: Double,
        targetPaceSecPerUnit: Double?,
        toleranceSecPerUnit: Double,
    ) : this(samples, splitDistanceMeters, targetPaceSecPerUnit, toleranceSecPerUnit, 5, null, null, null)
}
