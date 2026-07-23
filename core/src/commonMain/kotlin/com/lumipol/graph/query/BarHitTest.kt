package com.lumipol.graph.query

import kotlin.math.floor

/**
 * 플롯 내 x(뷰 픽셀)를 균등 슬롯 막대 인덱스로 변환(양 플랫폼 공유).
 *
 * 각 막대는 폭 `plotWidth/count`의 등거리 슬롯을 차지한다. 경계 밖 x는 `0..count-1`로 클램프한다.
 * iOS `RDBarChartView.barIndex(atX:)` 이식 — 롱프레스 스크럽 히트테스트.
 *
 * @return 0..count-1. `count <= 0` 또는 `plotWidth <= 0`이면 null.
 */
fun barIndexAtX(x: Double, plotMinX: Double, plotWidth: Double, count: Int): Int? {
    if (count <= 0 || plotWidth <= 0.0) return null
    val slot = plotWidth / count
    val raw = floor((x - plotMinX) / slot).toInt()
    return raw.coerceIn(0, count - 1)
}
