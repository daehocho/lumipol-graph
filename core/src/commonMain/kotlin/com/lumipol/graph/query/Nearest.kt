package com.lumipol.graph.query

import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.NearestResult
import kotlin.math.abs

fun nearest(data: LineChartData, x: Double): List<NearestResult> =
    data.series.mapNotNull { s ->
        val p = s.points.minByOrNull { abs(it.x - x) } ?: return@mapNotNull null
        NearestResult(s.id, p.x, p.y)
    }
