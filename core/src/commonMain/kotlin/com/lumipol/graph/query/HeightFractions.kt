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
 *
 * NaN 규칙(0.49.0): min/max 스캔에서 NaN은 **무시**한다 — `LineChartEngine.overlayAxisTicks`의
 * 고도 눈금 min/max와 같은 의미로 맞춘 것. 종전 `minOrNull()`은 NaN을 전파해 값 하나만 오염돼도
 * 전 fraction이 NaN(실루엣 전체 소실)이 됐고, 눈금은 정상 2개를 내 "라벨-실루엣 정렬" 불변식이
 * 깨졌다. 이제 오염된 점만 NaN fraction이 된다. 전부 NaN이면 평지와 같이 모두 0.
 * ±Inf는 종전대로 전파(눈금 쪽 비교 의미와 동일).
 */
fun heightFractions(values: List<Double>, minSpan: Double = 0.0): List<Double> {
    if (values.isEmpty()) return emptyList()
    var lo = Double.POSITIVE_INFINITY
    var hi = Double.NEGATIVE_INFINITY
    for (v in values) {
        if (v.isNaN()) continue
        if (v < lo) lo = v
        if (v > hi) hi = v
    }
    if (lo > hi) return values.map { 0.0 } // 전부 NaN
    val span = hi - lo
    if (span <= 0) return values.map { 0.0 }
    return values.map { (it - lo) / maxOf(span, minSpan) }
}
