package com.lumipol.graph.scale

import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.SeriesRole

/** 값-공간 정규화: 0.0 = min, 1.0 = max. 화면 반전은 렌더러 책임. */
data class AxisDomain(val min: Double, val max: Double) {
    fun normalize(v: Double): Double =
        if (max == min) 0.5 else (v - min) / (max - min)
}

/** 해당 Y축에 걸리는 모든 값(시리즈 점 + 밴드 경계)을 순서대로 모음.
 *  [xWindow]가 주어지면 시리즈 점은 창 안의 것만 (밴드는 항상 포함 — 축에서 사라지지 않게). */
fun yValues(data: LineChartData, axis: Axis, xWindow: AxisDomain? = null): List<Double> = buildList {
    data.series.filter { it.axis == axis && it.role != SeriesRole.OVERLAY }.forEach { s ->
        s.points.forEach { p ->
            if (xWindow == null || (p.x >= xWindow.min && p.x <= xWindow.max)) add(p.y)
        }
    }
    data.referenceBands.filter { it.axis == axis }.forEach { add(it.lower); add(it.upper) }
}
