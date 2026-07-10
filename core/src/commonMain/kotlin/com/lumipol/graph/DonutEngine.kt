package com.lumipol.graph

import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutChartLayout
import com.lumipol.graph.model.DonutSegmentLayout

/** 도넛 세그먼트 각도(fraction) 계산. value<=0 세그먼트는 제외. 순수 함수. */
object DonutEngine {
    fun layout(data: DonutChartData): DonutChartLayout {
        val valid = data.segments.withIndex().filter { it.value.value > 0.0 }
        val total = valid.sumOf { it.value.value }
        if (valid.isEmpty() || total <= 0.0) {
            return DonutChartLayout(emptyList(), 0.0)
        }
        var cursor = 0.0
        val out = valid.map { (index, seg) ->
            val sweep = seg.value / total
            val layout = DonutSegmentLayout(
                startFraction = cursor,
                sweepFraction = sweep,
                value = seg.value,
                colorRole = seg.colorRole,
                sourceIndex = index,
            )
            cursor += sweep
            layout
        }
        return DonutChartLayout(out, total)
    }
}
