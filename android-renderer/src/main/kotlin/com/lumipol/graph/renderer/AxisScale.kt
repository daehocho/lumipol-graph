// iOS: (대응 파일 없음 — 구 AxisScale.swift는 0.30.0 도메인 출력 채택과 함께 소거)
package com.lumipol.graph.renderer

import com.lumipol.graph.model.AxisTick
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartLayout

// 구 `AxisScale`(tick 두 점에서 정규화↔도메인 선형관계를 역산)은 0.30.0에서 제거됐다 —
// 코어가 `LineChartLayout.domains`로 계산에 쓴 도메인을 직접 출력하므로(경계 정책 §4-1
// "역산 금지") 렌더러는 `AxisDomain.normalize`/`denormalize`를 그대로 쓴다.

/**
 * [axis]의 tick 목록(없으면 null) — 터치마커/그리드가 같은 선택 규칙을 공유한다
 * (인라인 복붙이 서로 어긋나는 것 방지).
 */
internal fun LineChartLayout.ticksFor(axis: ChartAxis): List<AxisTick>? =
    axisTicks.firstOrNull { it.axis == axis }?.ticks
