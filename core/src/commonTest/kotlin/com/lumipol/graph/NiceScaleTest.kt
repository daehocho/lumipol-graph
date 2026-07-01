package com.lumipol.graph

import com.lumipol.graph.scale.niceScale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NiceScaleTest {
    @Test
    fun rounds_range_to_nice_bounds_and_ticks() {
        val s = niceScale(0.0, 97.0, maxTicks = 5)
        assertEquals(0.0, s.niceMin, 1e-9)
        assertEquals(100.0, s.niceMax, 1e-9)
        assertEquals(20.0, s.step, 1e-9)
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), s.ticks)
    }

    @Test
    fun handles_flat_data_without_crashing() {
        val s = niceScale(5.0, 5.0)
        assertTrue(s.niceMin < 5.0 && s.niceMax > 5.0)
        assertTrue(s.ticks.isNotEmpty())
    }

    @Test
    fun handles_reversed_min_max() {
        val s = niceScale(97.0, 0.0)
        assertEquals(0.0, s.niceMin, 1e-9)
        assertEquals(100.0, s.niceMax, 1e-9)
    }
}
