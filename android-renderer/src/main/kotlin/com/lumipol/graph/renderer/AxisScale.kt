// iOS: AxisScale.swift
package com.lumipol.graph.renderer

import com.lumipol.graph.model.AxisTick
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartLayout

/**
 * [axis]의 tick 목록(없으면 null) — 제스처/줌초기화/터치마커/그리드가 같은 선택 규칙을 공유한다
 * (인라인 복붙 4곳이 서로 어긋나는 것 방지).
 */
internal fun LineChartLayout.ticksFor(axis: ChartAxis): List<AxisTick>? =
    axisTicks.firstOrNull { it.axis == axis }?.ticks

/**
 * 축 tick의 (value, position) 선형 관계로 원본 값 ↔ 정규화 위치(0~1)를 상호 변환한다.
 *
 * X축은 데이터 맞춤 도메인이라 끝 tick이 position 1 안쪽일 수 있지만,
 * tick(value, position)은 항상 같은 선형 관계 위에 있으므로 양끝 tick으로 기울기를 얻는 방식은 유효하다.
 * Compose 비의존 순수 클래스 — 생성 실패 시 [from]이 null을 반환한다.
 */
internal class AxisScale private constructor(
    private val baseValue: Double,
    private val basePosition: Double,
    private val valuePerPosition: Double,
) {
    fun value(atPosition: Double): Double =
        baseValue + (atPosition - basePosition) * valuePerPosition

    fun position(ofValue: Double): Double =
        basePosition + (ofValue - baseValue) / valuePerPosition

    companion object {
        /** tick이 2개 미만이거나 위치·값 간격이 0이면 변환 불능 → null. */
        fun from(ticks: List<AxisTick>): AxisScale? {
            val first = ticks.firstOrNull() ?: return null
            val last = ticks.lastOrNull() ?: return null
            if (last.position == first.position || last.value == first.value) return null
            return AxisScale(
                baseValue = first.value,
                basePosition = first.position,
                valuePerPosition = (last.value - first.value) / (last.position - first.position),
            )
        }
    }
}
