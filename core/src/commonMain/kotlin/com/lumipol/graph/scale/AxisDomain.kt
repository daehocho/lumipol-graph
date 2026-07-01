package com.lumipol.graph.scale

import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.LineChartData

/** 값-공간 정규화: 0.0 = min, 1.0 = max. 화면 반전은 렌더러 책임. */
data class AxisDomain(val min: Double, val max: Double) {
    fun normalize(v: Double): Double =
        if (max == min) 0.5 else (v - min) / (max - min)
}

/** 해당 Y축에 걸리는 모든 값(시리즈 점 + 기준선 + 밴드 경계)을 순서대로 모음. */
fun yValues(data: LineChartData, axis: Axis): List<Double> = buildList {
    data.series.filter { it.axis == axis }.forEach { s -> s.points.forEach { add(it.y) } }
    data.referenceLines.filter { it.axis == axis }.forEach { add(it.value) }
    data.referenceBands.filter { it.axis == axis }.forEach { add(it.lower); add(it.upper) }
}
