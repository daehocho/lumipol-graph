package com.lumipol.graph

import com.lumipol.graph.model.*
import com.lumipol.graph.query.nearest
import kotlin.test.Test
import kotlin.test.assertEquals

class NearestTest {
    private val data = LineChartData(
        series = listOf(
            Series("pace", listOf(Point(0.0, 5.0), Point(1.0, 6.0), Point(2.0, 4.0))),
            Series("hr", listOf(Point(0.0, 150.0), Point(1.0, 160.0), Point(2.0, 170.0)), axis = Axis.SECONDARY),
        ),
    )

    @Test
    fun returns_nearest_point_per_series() {
        val r = nearest(data, 0.9)
        assertEquals(2, r.size)
        assertEquals(NearestResult("pace", 1.0, 6.0), r[0])
        assertEquals(NearestResult("hr", 1.0, 160.0), r[1])
    }

    @Test
    fun skips_empty_series() {
        val d = LineChartData(series = listOf(Series("empty", emptyList())))
        assertEquals(0, nearest(d, 1.0).size)
    }
}
