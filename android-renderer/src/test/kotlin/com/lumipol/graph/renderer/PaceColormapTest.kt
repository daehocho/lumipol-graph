package com.lumipol.graph.renderer

import androidx.compose.ui.graphics.Color
import com.lumipol.graph.model.BarColorRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// iOS: PaceColormapTests.swift — 공식 값 검증.
class PaceColormapTest {
    private fun input(value: Double, fastest: Double, slowest: Double, average: Double) =
        BarPaceColorInput(value, fastest, slowest, average, isPartial = false, index = 0,
            colorRole = BarColorRole.ON_TARGET)

    // fastest==slowest 축퇴 → 중간 초록 폴백.
    @Test fun degenerateIsGreen() {
        val c = ChartStyle.defaultPaceColor(input(300.0, 300.0, 300.0, 300.0))
        assertEquals(Color(red = 0f, green = 1f, blue = 0f), c)
    }

    // 가장 빠른 값(=fastest) → 파랑 구간 상단(blue=1, green 감쇠 최대).
    @Test fun fastestIsBlue() {
        // f=200, s=400, a=300 → pace1 = 300-(300-200)*0.70 = 230. p=200 < pace1.
        val c = ChartStyle.defaultPaceColor(input(200.0, 200.0, 400.0, 300.0))
        assertEquals(0f, c.red)
        assertEquals(1f, c.blue)
        assertTrue(c.green in 0.55f..0.65f) // 1 - 0.4*cv, cv≈1 → ~0.6
    }

    // 가장 느린 값(=slowest) → 빨강(red=1, green=0, blue=0).
    @Test fun slowestIsRed() {
        // f=200,s=400,a=300 → pace2 = 300+(400-300)*0.25 = 325. p=400 ≥ pace2, cv=(400-400)/(400-325)=0.
        val c = ChartStyle.defaultPaceColor(input(400.0, 200.0, 400.0, 300.0))
        assertEquals(1f, c.red)
        assertEquals(0f, c.green)
        assertEquals(0f, c.blue)
    }

    // 평균 근처(초록↔노랑 구간): pace1 ≤ p < pace2 → green=1.
    @Test fun midIsGreenYellowBand() {
        val c = ChartStyle.defaultPaceColor(input(300.0, 200.0, 400.0, 300.0)) // 230 ≤ 300 < 325
        assertEquals(1f, c.green)
        assertEquals(0f, c.blue)
        assertTrue(c.red in 0f..1f)
    }
}
