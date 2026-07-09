package com.lumipol.graph

import com.lumipol.graph.model.Point
import com.lumipol.graph.query.interpolatedY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** iOS 렌더러 `RDChartView.backgroundValue`에서 이관한 배경 area 보간 규약 테스트 (0.9.0). */
class AreaInterpolationTest {

    private fun assertClose(expected: Double, actual: Double?, tolerance: Double = 1e-9) {
        assertEquals(expected, actual!!, tolerance)
    }

    @Test
    fun clamps_to_endpoints_outside_range() {
        val points = listOf(Point(1.0, 10.0), Point(3.0, 30.0))
        assertClose(10.0, interpolatedY(points, 0.0))
        assertClose(10.0, interpolatedY(points, 1.0)) // 끝점 정확히
        assertClose(30.0, interpolatedY(points, 3.0)) // 끝점 정확히
        assertClose(30.0, interpolatedY(points, 5.0))
    }

    @Test
    fun interpolates_midpoint_linearly() {
        val points = listOf(Point(0.0, 0.0), Point(5.0, 100.0))
        assertClose(50.0, interpolatedY(points, 2.5))
        // 세 구간 중 두 번째 구간 내 보간
        val multi = listOf(Point(0.0, 0.0), Point(2.0, 20.0), Point(4.0, 0.0))
        assertClose(10.0, interpolatedY(multi, 3.0))
    }

    @Test
    fun empty_returns_null() {
        assertNull(interpolatedY(emptyList(), 1.0))
    }

    @Test
    fun single_point_clamps_both_sides() {
        val points = listOf(Point(2.0, 7.0))
        assertClose(7.0, interpolatedY(points, 0.0))
        assertClose(7.0, interpolatedY(points, 2.0))
        assertClose(7.0, interpolatedY(points, 9.0))
    }

    @Test
    fun exact_interior_point_and_duplicate_x() {
        // 내부 포인트 정확히 일치 + 같은 x가 연속(dx=0)인 경우 — 탐색 구현 교체 시 회귀 방지.
        val points = listOf(
            Point(0.0, 0.0), Point(1.0, 10.0),
            Point(1.0, 20.0), Point(2.0, 40.0),
        )
        assertClose(10.0, interpolatedY(points, 1.0))
        assertClose(30.0, interpolatedY(points, 1.5))
        // 많은 포인트에서 각 구간 중앙값 검증
        val many = (0..100).map { Point(it.toDouble(), it * 2.0) }
        assertClose(75.0, interpolatedY(many, 37.5))
        assertClose(198.0, interpolatedY(many, 99.0))
    }
}
