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

    @Test
    fun windowed_nearest_only_considers_points_inside_window() {
        // 창 [0, 5], 점 4.4(안)·5.3(밖): 전역 최근접은 5.3이지만 창 안 4.4가 답이어야 한다.
        val d = LineChartData(
            series = listOf(Series("pace", listOf(Point(0.0, 6.0), Point(4.4, 5.5), Point(5.3, 5.2)))),
        )
        val r = nearest(d, 5.0, xMin = 0.0, xMax = 5.0)
        assertEquals(listOf(NearestResult("pace", 4.4, 5.5)), r)
    }

    @Test
    fun windowed_nearest_skips_series_with_no_points_in_window() {
        val d = LineChartData(
            series = listOf(
                Series("pace", listOf(Point(4.0, 5.5))),
                Series("prev", listOf(Point(6.0, 6.2))),
            ),
        )
        val r = nearest(d, 4.5, xMin = 0.0, xMax = 5.0)
        assertEquals(listOf(NearestResult("pace", 4.0, 5.5)), r)
    }
}
