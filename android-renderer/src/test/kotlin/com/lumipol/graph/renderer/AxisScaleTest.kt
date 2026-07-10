package com.lumipol.graph.renderer

import com.lumipol.graph.model.AxisTick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// iOS: AxisScaleTests.swift
class AxisScaleTest {
    private val eps = 1e-9

    @Test
    fun mapsValueAndPositionLinearly() {
        val scale = AxisScale.from(
            listOf(
                AxisTick(value = 0.0, position = 0.0),
                AxisTick(value = 2.0, position = 0.5),
                AxisTick(value = 4.0, position = 1.0),
            ),
        )!!
        assertEquals(3.0, scale.value(atPosition = 0.75), eps)
        assertEquals(0.25, scale.position(ofValue = 1.0), eps)
    }

    @Test
    fun nonZeroBasePosition() {
        val scale = AxisScale.from(
            listOf(
                AxisTick(value = 10.0, position = 0.2),
                AxisTick(value = 20.0, position = 0.8),
            ),
        )!!
        assertEquals(15.0, scale.value(atPosition = 0.5), eps)
        assertEquals(0.35, scale.position(ofValue = 12.5), eps)
    }

    @Test
    fun returnsNullForDegenerateTicks() {
        assertNull(AxisScale.from(emptyList()))
        assertNull(AxisScale.from(listOf(AxisTick(value = 1.0, position = 0.5))))
        assertNull(
            AxisScale.from(
                listOf(
                    AxisTick(value = 1.0, position = 0.5),
                    AxisTick(value = 2.0, position = 0.5),
                ),
            ),
        )
        assertNull(
            AxisScale.from(
                listOf(
                    AxisTick(value = 3.0, position = 0.0),
                    AxisTick(value = 3.0, position = 1.0),
                ),
            ),
        )
    }
}
