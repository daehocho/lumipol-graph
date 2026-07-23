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
        xAxisLabels: List<String>? = null,
        yLabelFormatter: ((Double) -> String)? = null,
    ) = buildBarChartLayers(layout, style, width, height, xAxisLabels, yLabelFormatter)

    private fun bars(layers: List<LineChartLayer>) =
        layers.filterIsInstance<RectLayer>().filter { it.name.startsWith("bar.") }

    private fun textCount(layers: List<LineChartLayer>) = layers.filterIsInstance<TextLayer>().size
    private fun texts(layers: List<LineChartLayer>) = layers.filterIsInstance<TextLayer>().map { it.text }

    private fun named(layers: List<LineChartLayer>, prefix: String) =
        layers.filterIsInstance<TextLayer>().filter { it.name.startsWith(prefix) }

    // iOS: 막대 위 정적 라벨 제거 — 값은 롱프레스 말풍선으로만 노출. barLabel 레이어가 없어야 함.
    @Test
    fun emitsNoStaticBarLabels() {
        val layers = buildBarChartLayers(
            sampleLayout(6), style, width, height,
            xAxisLabels = null, yLabelFormatter = null,
        )
        assertTrue(named(layers, "barLabel.").isEmpty())
    }

    // x축 인덱스 라벨은 존치(솎아내기 포함).
    @Test
    fun keepsXAxisLabels() {
        val x = List(6) { "${it + 1}" }
        val layers = buildBarChartLayers(
            sampleLayout(6), style, width, height,
            xAxisLabels = x, yLabelFormatter = null,
        )
        assertTrue(named(layers, "barXLabel.").isNotEmpty())
    }

    @Test
    fun thinsXAxisLabelsIndependentlyOfBarLabels() {
        val x = List(20) { "${it + 1}" }
        val layers = buildBarChartLayers(
            sampleLayout(20), style, width, height,
            xAxisLabels = x, yLabelFormatter = null,
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
        val layers = build(sampleLayout(2), style = custom, xAxisLabels = listOf("1", "2"))
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

    // provider가 있으면 그 색을 그대로 사용.
    @Test
    fun barColorProviderTakesPrecedence() {
        val provided = Color(0xFF123456)
        val s = style.copy(barColorProvider = { provided })
        val layers = buildBarChartLayers(
            sampleLayout(4), s, width, height, xAxisLabels = null,
        )
        assertTrue(bars(layers).all { it.color == provided })
    }

    // provider 없으면 defaultPaceColor(연속) 사용 — 3버킷 barColors가 아님.
    @Test
    fun fallsBackToContinuousPalette() {
        // 값이 서로 다른 온전 스플릿 → 가장 빠른(작은 value) 막대는 파랑(blue=1), 가장 느린 막대는 빨강.
        val bars = listOf(
            BarLayout(0, value = 200.0, heightFraction = 0.2, colorRole = BarColorRole.ON_TARGET, isPartial = false),
            BarLayout(1, value = 300.0, heightFraction = 0.5, colorRole = BarColorRole.ON_TARGET, isPartial = false),
            BarLayout(2, value = 400.0, heightFraction = 0.9, colorRole = BarColorRole.ON_TARGET, isPartial = false),
        )
        val layout = BarChartLayout(bars = bars,
            yTicks = listOf(AxisTick(200.0, 0.0), AxisTick(400.0, 1.0)), referenceLinePosition = null)
        val rects = bars(buildBarChartLayers(layout, style, width, height))
        assertEquals(1f, rects[0].color.blue)   // 가장 빠름 → 파랑 구간
        assertEquals(1f, rects[2].color.red)    // 가장 느림 → 빨강
        assertEquals(0f, rects[2].color.blue)
    }

    // 앵커는 온전 스플릿만: 부분 스플릿의 극단값이 팔레트를 왜곡하지 않는다.
    @Test
    fun colorAnchorsUseFullSplitsOnly() {
        // 온전 2개(300,360) 범위 있음. 부분 1개(value=600, 아주 느림)는 앵커에서 제외.
        val bars = listOf(
            BarLayout(0, 300.0, 0.3, BarColorRole.ON_TARGET, isPartial = false),
            BarLayout(1, 360.0, 0.6, BarColorRole.ON_TARGET, isPartial = false),
            BarLayout(2, 600.0, 0.9, BarColorRole.ON_TARGET, isPartial = true),
        )
        val layout = BarChartLayout(bars = bars,
            yTicks = listOf(AxisTick(300.0, 0.0), AxisTick(360.0, 1.0)), referenceLinePosition = null)
        val rects = bars(buildBarChartLayers(layout, style, width, height))
        // slowest 앵커=360이므로 600은 노랑↔빨강 구간 상단(빨강)으로 클램프.
        assertEquals(1f, rects[2].color.red)
        // index1(360)은 온전 스플릿만 앵커로 쓸 때 slowest → 빨강. 부분(600) 포함 시 초록↔노랑으로 갈려 실패.
        assertEquals(1f, rects[1].color.red)
        assertEquals(0f, rects[1].color.green)
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

    // barColors(3버킷)는 하위호환용으로 존치되나 기본 색 경로는 연속 팔레트를 쓴다 — colorRole은
    // 알파(흐림)에만 영향을 주고 색 자체는 barColors를 참조하지 않는다.
    @Test
    fun partialBarIsDimmedRegardlessOfLegacyColorRole() {
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
        assertTrue(bars[2].alpha < 1f) // 부분막대 흐림
        assertEquals(1f, bars[0].alpha)
        assertEquals(1f, bars[1].alpha)
        // 색은 barColors 3버킷이 아니라 연속 팔레트에서 나온다. 앵커는 온전 스플릿(bar0=250, bar1=300)만
        // 사용(fastest=250, slowest=300, average=275) — 부분 스플릿 bar2(350)는 앵커 계산엔 빠지고
        // 색 클램프 대상으로만 쓰인다.
        for ((i, bar) in layout.bars.withIndex()) {
            val expected = ChartStyle.defaultPaceColor(
                BarPaceColorInput(
                    value = bar.value, fastest = 250.0, slowest = 300.0, average = 275.0,
                    isPartial = bar.isPartial, index = i, colorRole = bar.colorRole,
                ),
            )
            assertEquals(expected, bars[i].color, "bar $i")
        }
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

    private fun applySel(
        layout: BarChartLayout, selectedIndex: Int, barLabels: List<String>?,
        w: Double = width, h: Double = height, textW: Double = 30.0, textH: Double = 12.0,
    ): List<LineChartLayer> {
        val base = buildBarChartLayers(layout, style, w, h)
        return applyBarSelection(base, layout, style, w, h, selectedIndex, barLabels, textW, textH)
    }

    // 미선택 막대는 barDimOpacity 배율로 흐려지고, 선택 막대는 원래 alpha 유지.
    @Test
    fun dimsUnselectedBars() {
        val layout = sampleLayout(4, refPos = null)
        val out = applySel(layout, selectedIndex = 1, barLabels = List(4) { "5'00\"" })
        val rects = out.filterIsInstance<RectLayer>().filter { it.name.startsWith("bar.") && !it.name.contains("selection") }
        // 선택 인덱스 1은 base alpha(비부분=1f), 나머지는 *0.35 (단 인덱스3은 partial base 0.6 * 0.35)
        assertEquals(1f, rects[1].alpha)
        assertEquals(1f * 0.35f, rects[0].alpha)
        assertEquals(0.6f * 0.35f, rects[3].alpha, 1e-4f) // 마지막=partial
    }

    // 선택 막대가 partial이면 base alpha(partialBarAlpha)를 그대로 유지하고 dim 배율을 곱하지 않는다.
    @Test
    fun selectedPartialBarKeepsBaseAlphaWithoutDim() {
        val layout = sampleLayout(4, refPos = null) // 인덱스3=partial(base alpha 0.6)
        val out = applySel(layout, selectedIndex = 3, barLabels = List(4) { "5'00\"" })
        val rects = out.filterIsInstance<RectLayer>().filter { it.name.startsWith("bar.") && !it.name.contains("selection") }
        // 선택된 partial 막대(인덱스3)는 dim 없이 base alpha(0.6) 그대로.
        assertEquals(0.6f, rects[3].alpha)
        // 미선택 비-partial(인덱스0)은 1f * 0.35.
        assertEquals(1f * 0.35f, rects[0].alpha)
    }

    // 선택 시 세로 가이드선과 말풍선(배경+텍스트) 레이어가 추가된다.
    @Test
    fun addsGuideAndCallout() {
        val out = applySel(sampleLayout(4, refPos = null), selectedIndex = 2, barLabels = List(4) { "5'00\"" })
        assertTrue(out.any { it.name == "bar.selection.line" })
        assertTrue(out.any { it.name == "bar.selection.bubble" })
        val text = out.filterIsInstance<TextLayer>().first { it.name == "bar.selection.text" }
        assertEquals("5'00\"", text.text)
    }

    // 말풍선은 막대 높이와 무관하게 항상 플롯 상단(plot.minY)에 고정.
    @Test
    fun calloutPinnedToPlotTop() {
        val layout = sampleLayout(4, refPos = null) // 인덱스0 heightFraction=0.3(짧음)
        val out = applySel(layout, selectedIndex = 0, barLabels = List(4) { "5'00\"" })
        val bubble = out.filterIsInstance<RectLayer>().first { it.name == "bar.selection.bubble" }
        val plot = PlotArea(width, height, style.plotInsets)
        assertEquals(plot.minY, bubble.minY, 1e-6)
    }

    // barLabels 없으면 말풍선 없음(가이드선은 존재).
    @Test
    fun noCalloutWithoutLabels() {
        val out = applySel(sampleLayout(4, refPos = null), selectedIndex = 1, barLabels = null)
        assertTrue(out.any { it.name == "bar.selection.line" })
        assertTrue(out.none { it.name == "bar.selection.bubble" })
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
