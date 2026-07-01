package com.lumipol.graph

import com.lumipol.graph.model.Point
import com.lumipol.graph.model.Series
import com.lumipol.graph.stats.seriesStat
import com.lumipol.graph.stats.segmentStats
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {
    private val s = Series("pace", listOf(
        Point(0.0, 10.0), Point(1.0, 20.0), Point(2.0, 30.0), Point(3.0, 40.0),
    ))

    @Test
    fun series_min_max_avg() {
        val stat = seriesStat(s)
        assertEquals(10.0, stat.min, 1e-9)
        assertEquals(40.0, stat.max, 1e-9)
        assertEquals(25.0, stat.avg, 1e-9)
    }

    @Test
    fun splits_into_equal_x_bins() {
        // x 범위 0..3, 2구간 → [0,1.5): (0.0,1.0) 점 10,20 / [1.5,3]: 점 30,40
        val segs = segmentStats(s, 2)
        assertEquals(2, segs.size)
        assertEquals(15.0, segs[0].avg, 1e-9)
        assertEquals(35.0, segs[1].avg, 1e-9)
    }

    @Test
    fun zero_count_yields_no_segments() {
        assertEquals(0, segmentStats(s, 0).size)
    }
}
