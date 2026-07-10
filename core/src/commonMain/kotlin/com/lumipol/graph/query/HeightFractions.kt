package com.lumipol.graph.query

/**
 * 배경 실루엣(고도 등) 높이 정규화 — 값들을 자체 min~max로 0~1 정규화한다.
 * 빈 배열 → 빈 배열, 단일/전부 동일 → **모두 0**(평지 표현·0으로 나눔 방지).
 * AxisDomain.normalize(축퇴 시 0.5)와 다른, 실루엣 전용의 의도된 의미론(iOS parity).
 */
fun heightFractions(values: List<Double>): List<Double> {
    val lo = values.minOrNull() ?: return emptyList()
    val hi = values.maxOrNull() ?: return emptyList()
    val span = hi - lo
    if (span <= 0) return values.map { 0.0 }
    return values.map { (it - lo) / span }
}
