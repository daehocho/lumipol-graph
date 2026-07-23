package com.lumipol.graph.renderer

import androidx.compose.ui.graphics.Color
import com.lumipol.graph.model.AxisTick
import com.lumipol.graph.model.BarChartLayout
import com.lumipol.graph.model.BarColorRole
import com.lumipol.graph.model.BarLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// iOS: BarChartViewTests.swift — CALayer 검증을 순수 buildBarChartLayers 검증으로 재구성.
class RDBarChartTest {

    private val style = ChartStyle.defaults(darkTheme = false)
    private val width = 320.0
    private val height = 200.0

    private fun sampleLayout(barCount: Int, refPos: Double? = 0.5): BarChartLayout {
        val bars = (0 until barCount).map { i ->
            BarLayout(
                index = i,
                value = 300.0,
                heightFraction = 0.3 + 0.1 * i,
                colorRole = BarColorRole.ON_TARGET,
                isPartial = i == barCount - 1,
            )
        }
        val ticks = listOf(AxisTick(300.0, 0.2), AxisTick(360.0, 0.8))
        return BarChartLayout(bars = bars, yTicks = ticks, referenceLinePosition = refPos)
    }

    private fun build(
        layout: BarChartLayout,
        style: ChartStyle = this.style,
        barLabels: List<String>? = null,
        xAxisLabels: List<String>? = null,
        yLabelFormatter: ((Double) -> String)? = null,
    ) = buildBarChartLayers(layout, style, width, height, barLabels, xAxisLabels, yLabelFormatter)

    private fun bars(layers: List<LineChartLayer>) =
        layers.filterIsInstance<RectLayer>().filter { it.name.startsWith("bar.") }

    private fun textCount(layers: List<LineChartLayer>) = layers.filterIsInstance<TextLayer>().size
    private fun texts(layers: List<LineChartLayer>) = layers.filterIsInstance<TextLayer>().map { it.text }

    private fun named(layers: List<LineChartLayer>, prefix: String) =
        layers.filterIsInstance<TextLayer>().filter { it.name.startsWith(prefix) }

    // iOS: 막대 위 정적 라벨 제거 — 값은 롱프레스 말풍선으로만 노출. barLabel 레이어가 없어야 함.
    @Test
    fun emitsNoStaticBarLabels() {
        val labels = List(6) { "5'30\"" }
        val layers = buildBarChartLayers(
            sampleLayout(6), style, width, height,
            barLabels = labels, xAxisLabels = null, yLabelFormatter = null,
        )
        assertTrue(named(layers, "barLabel.").isEmpty())
    }

    // x축 인덱스 라벨은 존치(솎아내기 포함).
    @Test
    fun keepsXAxisLabels() {
        val x = List(6) { "${it + 1}" }
        val layers = buildBarChartLayers(
            sampleLayout(6), style, width, height,
            barLabels = null, xAxisLabels = x, yLabelFormatter = null,
        )
        assertTrue(named(layers, "barXLabel.").isNotEmpty())
    }

    @Test
    fun thinsXAxisLabelsIndependentlyOfBarLabels() {
        val x = List(20) { "${it + 1}" }
        val layers = buildBarChartLayers(
            sampleLayout(20), style, width, height,
            barLabels = null, xAxisLabels = x, yLabelFormatter = null,
            xLabelWidthPx = 40.0,
        )
        val shown = named(layers, "barXLabel.")
        assertTrue(shown.size < x.size, "x축 라벨도 솎아냄, 실제 ${shown.size}/20")
        assertTrue(shown.any { it.name == "barXLabel.0" })
        assertTrue(shown.any { it.name == "barXLabel.19" }, "마지막 x축 라벨 표시")
    }

    @Test
    fun rendersOneLayerPerBar() {
        assertEquals(4, bars(build(sampleLayout(4))).size)
    }

    @Test
    fun axisLabelFontFamilyAndWeightPropagateToBarLabels() {
        // QA Minor-6: 바 차트 라벨(y축·막대 위·x축)도 ChartStyle 주입 폰트를 실어야 한다(iOS
        // RDBarChartView가 style.axisLabelFont를 그대로 쓰는 것과 동일).
        val custom = style.copy(
            axisLabelFontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            axisLabelFontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        )
        val layers = build(sampleLayout(2), style = custom, barLabels = listOf("a", "b"), xAxisLabels = listOf("1", "2"))
        val labels = layers.filterIsInstance<TextLayer>()
        assertTrue(labels.isNotEmpty())
        labels.forEach { label ->
            assertEquals(androidx.compose.ui.text.font.FontFamily.Monospace, label.fontFamily, label.name)
            assertEquals(androidx.compose.ui.text.font.FontWeight.Medium, label.fontWeight, label.name)
        }
    }

    @Test
    fun barWidthRatioAndPartialAlphaAreConfigurableViaStyle() {
        // 슬롯 대비 막대 폭 비율·부분 스플릿 알파는 하드코딩이 아니라 ChartStyle 소유(iOS 스타일과
        // 한 곳에서 동기화). ratio=1.0이면 막대 폭 == 슬롯 폭.
        val custom = style.copy(barWidthRatio = 1.0f, partialBarAlpha = 0.25f)
        val layers = build(sampleLayout(2), style = custom)
        val plot = PlotArea(width, height, custom.plotInsets)
        val bar = bars(layers).first { it.name == "bar.0" }
        assertEquals(plot.width / 2, bar.width, 1e-6)
        val partial = bars(layers).first { it.name == "bar.1" } // sampleLayout: 마지막 막대가 partial
        assertEquals(0.25f, partial.alpha)
    }

    @Test
    fun missingColorRoleFallsBackToStyleFallbackColor() {
        // 색 role이 주입 맵에 없으면 하드코딩 회색이 아니라 ChartStyle.fallbackDataColor를 쓴다.
        val custom = style.copy(barColors = emptyMap(), fallbackDataColor = Color.Magenta)
        val bar = bars(build(sampleLayout(1), style = custom)).first()
        assertEquals(Color.Magenta, bar.color)
    }

    @Test
    fun emptyLayoutRendersNoBars() {
        val layers = build(BarChartLayout(bars = emptyList(), yTicks = emptyList(), referenceLinePosition = null))
        assertEquals(0, bars(layers).size)
        assertTrue(layers.isEmpty())
    }

    @Test
    fun barCountMatchesLayout() {
        assertEquals(2, bars(build(sampleLayout(2))).size)
    }

    @Test
    fun barsCarryStyleCornerRadius() {
        // Minor-1 회귀 가드: 바 RectLayer는 style.barCornerRadius(iOS barCornerRadius 3dp)를 실어야
        // render가 drawRoundRect로 둥근 모서리를 그린다. cornerRadius가 소실되면 사각으로 렌더된다.
        assertTrue(style.barCornerRadius > 0f)
        val bars = bars(build(sampleLayout(3)))
        assertTrue(bars.isNotEmpty())
        assertTrue(bars.all { it.cornerRadius == style.barCornerRadius })

        // 스케일된 스타일에서도 값이 전파되는지(density 스케일 포함) 확인.
        val scaled = style.scaledForDensity(2f)
        val scaledBars = bars(build(sampleLayout(2), style = scaled))
        assertTrue(scaledBars.all { it.cornerRadius == scaled.barCornerRadius })
    }

    @Test
    fun barColorMatchesRoleAndPartialIsDimmed() {
        val custom = style.copy(
            barColors = mapOf(
                BarColorRole.FASTER to Color.Red,
                BarColorRole.ON_TARGET to Color.Green,
                BarColorRole.SLOWER to Color.Blue,
            ),
        )
        val layout = BarChartLayout(
            bars = listOf(
                BarLayout(0, 250.0, 0.8, BarColorRole.FASTER, false),
                BarLayout(1, 300.0, 0.5, BarColorRole.ON_TARGET, false),
                BarLayout(2, 350.0, 0.3, BarColorRole.SLOWER, true),
            ),
            yTicks = emptyList(),
            referenceLinePosition = null,
        )
        val bars = bars(build(layout, style = custom))
        assertEquals(3, bars.size)
        assertEquals(Color.Red, bars[0].color)
        assertEquals(Color.Green, bars[1].color)
        assertEquals(Color.Blue, bars[2].color)
        assertTrue(bars[2].alpha < 1f) // 부분막대 흐림
        assertEquals(1f, bars[0].alpha)
    }

    @Test
    fun yAxisLabelsHiddenWhenFlagFalse() {
        val layers = build(sampleLayout(3), style = style.copy(barShowYAxisLabels = false))
        assertEquals(0, textCount(layers))
    }

    @Test
    fun yAxisLabelsShownWhenFlagTrue() {
        val layers = build(sampleLayout(3), style = style.copy(barShowYAxisLabels = true))
        assertEquals(2, textCount(layers)) // yTicks 2개
    }

    @Test
    fun yLabelFormatterIsApplied() {
        val layout = BarChartLayout(
            bars = listOf(BarLayout(0, 300.0, 0.5, BarColorRole.ON_TARGET, false)),
            yTicks = listOf(AxisTick(300.0, 0.5)),
            referenceLinePosition = null,
        )
        val layers = build(layout, yLabelFormatter = { "P${it.toInt()}" })
        assertTrue(texts(layers).contains("P300"))
    }

    @Test
    fun xAxisLabelsDrawnUnderBars() {
        val layout = BarChartLayout(
            bars = listOf(
                BarLayout(0, 300.0, 0.5, BarColorRole.ON_TARGET, false),
                BarLayout(1, 310.0, 0.6, BarColorRole.SLOWER, false),
            ),
            yTicks = emptyList(),
            referenceLinePosition = null,
        )
        val texts = texts(build(layout, xAxisLabels = listOf("1", "2")))
        assertTrue(texts.contains("1"))
        assertTrue(texts.contains("2"))
    }

    @Test
    fun xAxisLabelsHiddenWhenFlagOff() {
        val layout = BarChartLayout(
            bars = listOf(BarLayout(0, 300.0, 0.5, BarColorRole.ON_TARGET, false)),
            yTicks = emptyList(),
            referenceLinePosition = null,
        )
        val texts = texts(build(layout, style = style.copy(barShowXAxisLabels = false), xAxisLabels = listOf("1")))
        assertFalse(texts.contains("1"))
    }

    @Test
    fun defaultsExposeBarSelectionFields() {
        val light = ChartStyle.defaults(darkTheme = false)
        assertEquals(0.35f, light.barDimOpacity)
        assertEquals(12f, light.barCalloutFontSize)
        // 라이트 말풍선 배경 = label(불투명 검정), 텍스트 = systemBackground(불투명 흰색)
        assertEquals(1f, light.barCalloutBackgroundColor.alpha)
        assertEquals(Color.White, light.barCalloutTextColor)
        assertEquals(null, light.barColorProvider)
        // 다크는 반전
        val dark = ChartStyle.defaults(darkTheme = true)
        assertEquals(Color.Black, dark.barCalloutTextColor)
    }
}
