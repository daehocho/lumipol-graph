package com.lumipol.graph.query

/**
 * 배경 실루엣(고도 등) 높이 정규화 — 값들을 자체 min~max로 0~1 정규화한다.
 * 빈 배열 → 빈 배열, 단일/전부 동일 → **모두 0**(평지 표현·0으로 나눔 방지).
 * AxisDomain.normalize(축퇴 시 0.5)와 다른, 실루엣 전용의 의도된 의미론(iOS parity).
 *
 * [minSpan]은 정규화 분모의 하한이다. 자체 min~max로만 나누면 고저차 0.2m든 200m든 똑같이
 * 플롯 전체 높이를 채워 센서 노이즈가 산맥으로 보인다. 실측 span이 [minSpan]보다 작으면
 * 그만큼 납작하게 그려진다(0이면 하한 없음 = 구 동작). 전처리에서 평지를 잘라내던 역할을
 * 여기로 옮긴 것 — "그릴지"와 "얼마나 크게 그릴지"는 다른 축이다.
 */
fun heightFractions(values: List<Double>, minSpan: Double = 0.0): List<Double> {
    val lo = values.minOrNull() ?: return emptyList()
    val hi = values.maxOrNull() ?: return emptyList()
    val span = hi - lo
    if (span <= 0) return values.map { 0.0 }
    return values.map { (it - lo) / maxOf(span, minSpan) }
}
