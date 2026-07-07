package com.lumipol.graph

import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutColorRole
import com.lumipol.graph.model.DonutSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DonutEngineTest {

    private fun seg(v: Double, role: DonutColorRole = DonutColorRole.ZONE1) = DonutSegment(v, role)

    @Test
    fun sweep_fractions_sum_to_one() {
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(30.0), seg(30.0, DonutColorRole.ZONE2), seg(40.0, DonutColorRole.ZONE3)))
        )
        val sum = layout.segments.sumOf { it.sweepFraction }
        assertEquals(1.0, sum, 1e-6)
        assertEquals(100.0, layout.total, 1e-6)
    }

    @Test
    fun start_fractions_are_cumulative() {
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(25.0), seg(75.0, DonutColorRole.ZONE2)))
        )
        assertEquals(0.0, layout.segments[0].startFraction, 1e-6)
        assertEquals(0.25, layout.segments[0].sweepFraction, 1e-6)
        assertEquals(0.25, layout.segments[1].startFraction, 1e-6)
        assertEquals(0.75, layout.segments[1].sweepFraction, 1e-6)
    }

    @Test
    fun zero_value_segments_are_dropped() {
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(0.0), seg(50.0, DonutColorRole.ZONE2), seg(0.0, DonutColorRole.ZONE3)))
        )
        assertEquals(1, layout.segments.size)
        assertEquals(DonutColorRole.ZONE2, layout.segments[0].colorRole)
        assertEquals(1.0, layout.segments[0].sweepFraction, 1e-6)
    }

    @Test
    fun empty_and_all_zero_input_returns_empty_layout() {
        val empty = DonutEngine.layout(DonutChartData(emptyList()))
        assertTrue(empty.segments.isEmpty())
        assertEquals(0.0, empty.total, 1e-6)

        val allZero = DonutEngine.layout(DonutChartData(listOf(seg(0.0), seg(0.0, DonutColorRole.ZONE2))))
        assertTrue(allZero.segments.isEmpty())
        assertEquals(0.0, allZero.total, 1e-6)
    }

    @Test
    fun fractions_are_in_unit_range() {
        val layout = DonutEngine.layout(
            DonutChartData(listOf(seg(10.0), seg(20.0, DonutColorRole.ZONE2), seg(70.0, DonutColorRole.ZONE3)))
        )
        layout.segments.forEach {
            assertTrue(it.startFraction in 0.0..1.0)
            assertTrue(it.sweepFraction in 0.0..1.0)
        }
    }
}
