package com.lumipol.graph.renderer

import com.lumipol.graph.model.AxisTick
import com.lumipol.graph.model.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// iOS: AreaSilhouetteTests.swift — heightFractions(순수) + area Path 빌더(build) 검증.
class AreaSilhouetteTest {
    @Test
    fun heightFractionsNormalizesToOwnMinMax() {
        assertEquals(listOf(0.0, 1.0, 0.5), AreaSilhouette.heightFractions(listOf(0.0, 10.0, 5.0)))
    }

    @Test
    fun heightFractionsEmptyInputReturnsEmpty() {
        assertEquals(emptyList(), AreaSilhouette.heightFractions(emptyList()))
    }

    @Test
    fun heightFractionsSinglePointIsFlat() {
        assertEquals(listOf(0.0), AreaSilhouette.heightFractions(listOf(42.0)))
    }

    @Test
    fun heightFractionsAllEqualIsFlatNoDivideByZero() {
        assertEquals(listOf(0.0, 0.0, 0.0), AreaSilhouette.heightFractions(listOf(3.0, 3.0, 3.0)))
    }

    // MARK: - Path 빌더

    private val style = ChartStyle.defaults(darkTheme = false)
    private val plot = PlotArea(100.0, 100.0, Insets(0f, 0f, 0f, 0f))
    private val xScale = AxisScale.from(listOf(AxisTick(0.0, 0.0), AxisTick(10.0, 1.0)))!!

    private fun bounds(polygon: List<PlotPoint>): DoubleArray {
        val xs = polygon.map { it.x }
        val ys = polygon.map { it.y }
        return doubleArrayOf(xs.min(), ys.min(), xs.max(), ys.max())
    }

    @Test
    fun buildSpansBottomFractionFromBaseline() {
        // 고도 0→10 (fraction 0→1), areaHeightFraction 0.35 → 봉우리 y = 65, 바닥 100
        val layer = AreaSilhouette.build(
            points = listOf(Point(0.0, 0.0), Point(10.0, 10.0)),
            xScale = xScale, plot = plot, style = style.copy(areaHeightFraction = 0.35f),
        )!!
        assertEquals("area.altitude", layer.name)
        val b = bounds(layer.polygon)
        assertEquals(0.0, b[0]) // minX
        assertEquals(65.0, b[1], 1e-3) // minY (봉우리; areaHeightFraction Float 정밀도 허용)
        assertEquals(100.0, b[2]) // maxX
        assertEquals(100.0, b[3]) // maxY (바닥)
        assertEquals(style.areaFillColor, layer.color)
    }

    @Test
    fun buildClampsPointsOutsideXDomainToPlotRect() {
        // 시리즈 x-도메인(0~10)보다 넓은 고도(-5~15) — 좌표를 플롯 영역으로 클램프.
        val layer = AreaSilhouette.build(
            points = listOf(Point(-5.0, 0.0), Point(5.0, 10.0), Point(15.0, 0.0)),
            xScale = xScale, plot = plot, style = style,
        )!!
        val b = bounds(layer.polygon)
        assertTrue(b[0] >= plot.minX)
        assertTrue(b[2] <= plot.maxX)
    }

    @Test
    fun buildAppliesStyleMinValueSpanToFlattenNoise() {
        // 고저차 0.25m. minSpan 없으면 봉우리가 usableHeight를 전부 채운다(노이즈가 산맥).
        // 기본 areaMinValueSpan=0.5가 build까지 흐르면 절반(0.5)까지만 올라간다.
        val noise = listOf(Point(0.0, 10.0), Point(10.0, 10.25))
        val usable = 0.35 * plot.height // 35

        val default = AreaSilhouette.build(noise, xScale, plot, style)!!
        assertEquals(100.0 - 0.5 * usable, bounds(default.polygon)[1], 1e-3) // 82.5

        val noFloor = AreaSilhouette.build(noise, xScale, plot, style.copy(areaMinValueSpan = 0.0))!!
        assertEquals(100.0 - usable, bounds(noFloor.polygon)[1], 1e-3) // 65.0

        // 실측 고저차가 하한보다 크면 하한은 관여하지 않는다.
        val real = AreaSilhouette.build(
            listOf(Point(0.0, 0.0), Point(10.0, 100.0)), xScale, plot, style,
        )!!
        assertEquals(100.0 - usable, bounds(real.polygon)[1], 1e-3)
    }

    @Test
    fun buildNullForFewerThanTwoPoints() {
        assertNull(
            AreaSilhouette.build(listOf(Point(0.0, 5.0)), xScale, plot, style),
        )
    }
}
