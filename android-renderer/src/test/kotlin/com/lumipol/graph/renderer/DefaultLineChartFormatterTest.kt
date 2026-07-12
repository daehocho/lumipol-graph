package com.lumipol.graph.renderer

import com.lumipol.graph.model.ChartAxis
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [defaultLineChartFormatter] ↔ iOS `String(format: "%g", value)` 출력 패리티(QA Minor-4).
 * 기대값은 C `%g`(macOS printf) 실측 — 유효숫자 6자리, 트레일링 0 제거, 지수 표기 전환 규칙을 고정한다.
 * Java `%g`는 트레일링 0을 유지하므로("5.00000") 정리 로직이 빠지면 전부 깨진다.
 */
class DefaultLineChartFormatterTest {

    private fun fmt(value: Double) = defaultLineChartFormatter(ChartAxis.X, value)

    @Test
    fun integersDropDecimalPoint() {
        assertEquals("5", fmt(5.0))
        assertEquals("42", fmt(42.0))
        assertEquals("0", fmt(0.0))
        assertEquals("-3", fmt(-3.0))
        assertEquals("123456", fmt(123456.0))
        assertEquals("999999", fmt(999999.0)) // 소수 표기의 상한(6유효숫자) 직전
    }

    @Test
    fun decimalsTrimTrailingZeros() {
        assertEquals("5.5", fmt(5.5))
        assertEquals("5.35", fmt(5.35))
        assertEquals("-12.3", fmt(-12.3))
        assertEquals("-0.5", fmt(-0.5))
    }

    @Test
    fun roundsToSixSignificantDigits() {
        assertEquals("42.1235", fmt(42.1235486))
        assertEquals("3.14159", fmt(Math.PI))
    }

    @Test
    fun largeValuesSwitchToExponentNotation() {
        assertEquals("1.23457e+06", fmt(1234567.89)) // QA 리포트 대표값
        assertEquals("1e+06", fmt(1_000_000.0))      // 지수 표기 진입 + 만티사 0 제거
        assertEquals("1.5e+06", fmt(1_500_000.0))
    }

    @Test
    fun smallValuesSwitchToExponentBelowMinusFour() {
        assertEquals("0.0001", fmt(0.0001)) // 지수 -4는 아직 소수 표기(C %g 경계)
        assertEquals("1e-05", fmt(0.00001))
    }

    @Test
    fun nonFiniteValuesMatchCNotation() {
        assertEquals("nan", fmt(Double.NaN))
        assertEquals("inf", fmt(Double.POSITIVE_INFINITY))
        assertEquals("-inf", fmt(Double.NEGATIVE_INFINITY))
    }
}
