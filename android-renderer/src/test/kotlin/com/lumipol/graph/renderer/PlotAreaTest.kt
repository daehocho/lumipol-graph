package com.lumipol.graph.renderer

import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.NormalizedPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// iOS: PlotAreaTests.swift
class PlotAreaTest {
    private val eps = 1e-9

    // rect = (x: 20, y: 10, w: 100, h: 200)  ←  size 120x220, insets top10/left20/bottom10/right0
    private val area = PlotArea(
        sizeWidth = 120.0,
        sizeHeight = 220.0,
        insets = Insets(top = 10f, left = 20f, bottom = 10f, right = 0f),
    )

    @Test
    fun xMapsIntoPlotRect() {
        assertEquals(20.0, area.x(0.0), eps)
        assertEquals(70.0, area.x(0.5), eps)
        assertEquals(120.0, area.x(1.0), eps)
    }

    @Test
    fun normalAxisPutsMaxValueAtTop() {
        // 정상 축: ny=1(축 최대) → 위(minY), ny=0(축 최소) → 아래(maxY)
        assertEquals(10.0, area.y(1.0, Axis.PRIMARY), eps)
        assertEquals(210.0, area.y(0.0, Axis.PRIMARY), eps)
        assertEquals(160.0, area.y(0.25, Axis.PRIMARY), eps)
    }

    @Test
    fun invertedAxisPutsMinValueAtTop() {
        val inverted = PlotArea(
            sizeWidth = 120.0,
            sizeHeight = 220.0,
            insets = Insets(top = 10f, left = 20f, bottom = 10f, right = 0f),
            invertedAxes = setOf(Axis.PRIMARY),
        )
        // 반전 축(페이스): ny=0(축 최소 = 빠른 페이스) → 위
        assertEquals(10.0, inverted.y(0.0, Axis.PRIMARY), eps)
        assertEquals(210.0, inverted.y(1.0, Axis.PRIMARY), eps)
        // 반전 대상이 아닌 축은 정상 방향 유지
        assertEquals(210.0, inverted.y(0.0, Axis.SECONDARY), eps)
    }

    @Test
    fun pointCombinesXAndY() {
        val p = area.point(NormalizedPoint(x = 0.5, y = 0.5), Axis.PRIMARY)
        assertEquals(PlotPoint(70.0, 110.0), p)
    }

    @Test
    fun normalizedXInvertsAndClamps() {
        assertEquals(0.0, area.normalizedX(20.0), eps)
        assertEquals(0.5, area.normalizedX(70.0), eps)
        assertEquals(1.0, area.normalizedX(120.0), eps)
        assertEquals(0.0, area.normalizedX(-5.0), eps)
        assertEquals(1.0, area.normalizedX(999.0), eps)
    }

    @Test
    fun zeroSizeBoundsIsNotRenderable() {
        val zero = PlotArea(0.0, 0.0, Insets(0f, 0f, 0f, 0f))
        assertFalse(zero.isRenderable)
        assertTrue(area.isRenderable)
    }

    @Test
    fun insetsLargerThanBoundsIsNotRenderable() {
        // size 0 + 기본 insets(top16/left44/bottom20/right44) → 음수 크기 → 렌더 불가
        val collapsed = PlotArea(0.0, 0.0, ChartStyle.defaults(darkTheme = false).plotInsets)
        assertFalse(collapsed.isRenderable)
    }
}
