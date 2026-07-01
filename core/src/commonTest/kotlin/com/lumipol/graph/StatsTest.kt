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

    @Test
    fun segment_count_reflects_points_per_bin() {
        val segs = segmentStats(s, 2)
        assertEquals(2, segs[0].count)
        assertEquals(2, segs[1].count)
    }

    @Test
    fun zero_span_puts_all_points_in_first_bin() {
        val flat = Series("flat", listOf(Point(5.0, 1.0), Point(5.0, 2.0), Point(5.0, 3.0)))
        val segs = segmentStats(flat, 2)
        assertEquals(2, segs.size)
        assertEquals(3, segs[0].count)
        assertEquals(0, segs[1].count)
        assertEquals(0.0, segs[1].min, 1e-9)
        assertEquals(0.0, segs[1].max, 1e-9)
        assertEquals(0.0, segs[1].avg, 1e-9)
    }

    @Test
    fun empty_series_guard() {
        val empty = Series("e", emptyList())
        val stat = seriesStat(empty)
        assertEquals("e", stat.id)
        assertEquals(0.0, stat.min, 1e-9)
        assertEquals(0.0, stat.max, 1e-9)
        assertEquals(0.0, stat.avg, 1e-9)
        assertEquals(0, segmentStats(empty, 3).size)
    }
}
