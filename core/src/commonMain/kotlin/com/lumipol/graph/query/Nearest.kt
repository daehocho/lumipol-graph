package com.lumipol.graph.query

import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.NearestResult
import kotlin.math.abs

fun nearest(data: LineChartData, x: Double): List<NearestResult> =
    data.series.mapNotNull { s ->
        val p = s.points.minByOrNull { abs(it.x - x) } ?: return@mapNotNull null
        NearestResult(s.id, p.x, p.y)
    }

/** [xMin, xMax] 창 안 점만 대상 — 창 밖 전역 최근접점이 창 안 이웃을 가리는 것을 막는다(줌 가장자리 스크럽). */
fun nearest(data: LineChartData, x: Double, xMin: Double, xMax: Double): List<NearestResult> =
    data.series.mapNotNull { s ->
        val p = s.points.filter { it.x in xMin..xMax }.minByOrNull { abs(it.x - x) }
            ?: return@mapNotNull null
        NearestResult(s.id, p.x, p.y)
    }
