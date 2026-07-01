package com.lumipol.graph.stats

import com.lumipol.graph.model.SegmentStat
import com.lumipol.graph.model.Series
import com.lumipol.graph.model.SeriesStat

fun seriesStat(series: Series): SeriesStat {
    if (series.points.isEmpty()) return SeriesStat(series.id, 0.0, 0.0, 0.0)
    val ys = series.points.map { it.y }
    return SeriesStat(series.id, ys.min(), ys.max(), ys.average())
}

/** X 범위를 [count] 등간격 구간으로 나눠 각 구간 y의 min/max/avg. */
fun segmentStats(series: Series, count: Int): List<SegmentStat> {
    if (count <= 0 || series.points.isEmpty()) return emptyList()
    val xs = series.points.map { it.x }
    val xMin = xs.min()
    val span = xs.max() - xMin
    val bins = List(count) { mutableListOf<Double>() }
    for (p in series.points) {
        val frac = if (span == 0.0) 0.0 else (p.x - xMin) / span
        val idx = (frac * count).toInt().coerceIn(0, count - 1)
        bins[idx].add(p.y)
    }
    return bins.map { b ->
        if (b.isEmpty()) SegmentStat(0.0, 0.0, 0.0)
        else SegmentStat(b.min(), b.max(), b.average())
    }
}
