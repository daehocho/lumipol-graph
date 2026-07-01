package com.lumipol.graph

import com.lumipol.graph.model.*
import com.lumipol.graph.scale.AxisDomain
import com.lumipol.graph.scale.yValues
import kotlin.test.Test
import kotlin.test.assertEquals

class AxisDomainTest {
    @Test
    fun normalizes_value_into_0_to_1() {
        val d = AxisDomain(0.0, 200.0)
        assertEquals(0.0, d.normalize(0.0), 1e-9)
        assertEquals(0.5, d.normalize(100.0), 1e-9)
        assertEquals(1.0, d.normalize(200.0), 1e-9)
    }

    @Test
    fun flat_domain_maps_to_mid() {
        val d = AxisDomain(50.0, 50.0)
        assertEquals(0.5, d.normalize(50.0), 1e-9)
    }

    @Test
    fun yValues_collects_series_reflines_and_bands_on_axis() {
        val data = LineChartData(
            series = listOf(
                Series("hr", listOf(Point(0.0, 150.0), Point(1.0, 170.0)), axis = Axis.SECONDARY),
                Series("pace", listOf(Point(0.0, 5.0)), axis = Axis.PRIMARY),
            ),
            referenceLines = listOf(RefLine(4.5, axis = Axis.PRIMARY)),
            referenceBands = listOf(RefBand(4.0, 4.2, axis = Axis.PRIMARY)),
        )
        assertEquals(listOf(5.0, 4.5, 4.0, 4.2), yValues(data, Axis.PRIMARY))
        assertEquals(listOf(150.0, 170.0), yValues(data, Axis.SECONDARY))
    }
}
