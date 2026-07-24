package com.lumipol.graph.renderer

import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.AxisTick
import com.lumipol.graph.model.AxisTicksLayout
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.ChartConfig
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.LineChartLayout
import com.lumipol.graph.model.MarkerLayout
import com.lumipol.graph.model.NormalizedPoint
import com.lumipol.graph.model.RefBandLayout
import com.lumipol.graph.model.Series
import com.lumipol.graph.model.SeriesLayout
import com.lumipol.graph.model.SeriesRole
import com.lumipol.graph.model.Stats
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// iOS: ChartLayerBuilderTests.swift — CALayer 트리 검증을 순수 중간 모델(LineChartLayer) 검증으로 재구성.
// 픽셀 대신 "레이어 이름·순서 + path 좌표/색 역할"을 단위 검증(Swift 분석 제안).
class LineChartDrawingTest {

    private val style = ChartStyle.defaults(darkTheme = false)

    private val layout = LineChartLayout(
        series = listOf(
            SeriesLayout("pace", SeriesRole.MAIN, listOf(NormalizedPoint(0.0, 0.0), NormalizedPoint(1.0, 1.0))),
            SeriesLayout("pace_prev", SeriesRole.GHOST, listOf(NormalizedPoint(0.0, 1.0), NormalizedPoint(1.0, 0.0))),
        ),
        axisTicks = listOf(
            AxisTicksLayout(ChartAxis.X, listOf(AxisTick(0.0, 0.0), AxisTick(5.0, 1.0))),
            AxisTicksLayout(ChartAxis.Y_PRIMARY, listOf(AxisTick(4.0, 0.0), AxisTick(6.0, 1.0))),
        ),
        refBands = listOf(RefBandLayout(Axis.PRIMARY, 0.25, 0.75)),
        markers = listOf(
            MarkerLayout(0.5, "1km", false),
            MarkerLayout(1.0, "2km", true),
        ),
        stats = Stats(emptyList(), emptyList(), null),
    )
    private val data = LineChartData(
        series = listOf(
            Series("pace", emptyList(), Axis.PRIMARY, SeriesRole.MAIN),
            Series("pace_prev", emptyList(), Axis.PRIMARY, SeriesRole.GHOST),
        ),
        config = ChartConfig(segmentCount = 0, maxTicks = 5),
    )
    private val plot = PlotArea(100.0, 100.0, Insets(0f, 0f, 0f, 0f))

    private fun build(
        layout: LineChartLayout = this.layout,
        data: LineChartData = this.data,
        style: ChartStyle = this.style,
        plot: PlotArea = this.plot,
        formatter: (ChartAxis, Double) -> String = { _, v -> "$v" },
    ) = buildLineChartLayers(layout, data, style, plot, formatter)

    private fun List<LineChartLayer>.named(name: String) = firstOrNull { it.name == name }

    private fun StrokeLayer.bounds(): FloatArray {
        val xs = segments.flatten().map { it.x }
        val ys = segments.flatten().map { it.y }
        return floatArrayOf(xs.min().toFloat(), ys.min().toFloat(), xs.max().toFloat(), ys.max().toFloat())
    }

    private fun ContainerLayer.strokeChild() = children.filterIsInstance<StrokeLayer>().first()
    private fun ContainerLayer.hasText() = children.any { it is TextLayer }

    @Test
    fun duplicateSeriesIdsDoNotCrash() {
        val dupData = LineChartData(
            series = listOf(
                Series("pace", emptyList(), Axis.PRIMARY, SeriesRole.MAIN),
                Series("pace", emptyList(), Axis.SECONDARY, SeriesRole.GHOST),
            ),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )
        assertTrue(build(data = dupData).isNotEmpty())
    }

    @Test
    fun buildsExpectedLayerTree() {
        assertEquals(
            listOf(
                "grid",
                "band.0", "marker.0", "marker.1",
                "series.ghost.pace_prev",
                "series.gradient.pace", "series.main.pace",
                "axisLabels.x", "axisLabels.yPrimary",
            ),
            build().map { it.name },
        )
    }

    @Test
    fun gridDrawsDashedLinesAtTicks() {
        val grid = build().named("grid") as StrokeLayer
        assertContentEquals(style.gridLineDashPattern, grid.dash)
        // x tick 2개(0,1) 세로선 + yPrimary tick 2개(0,1) 가로선 → 플롯 전체 바운딩
        assertContentEquals(floatArrayOf(0f, 0f, 100f, 100f), grid.bounds())
    }

    @Test
    fun nilGridColorOmitsGridLayer() {
        assertNull(build(style = style.copy(gridLineColor = null)).named("grid"))
    }

    @Test
    fun gradientOnlyOnPrimaryAxisSeries() {
        val dualLayout = LineChartLayout(
            series = listOf(
                SeriesLayout("pace", SeriesRole.MAIN, listOf(NormalizedPoint(0.0, 0.0), NormalizedPoint(1.0, 1.0))),
                SeriesLayout("hr", SeriesRole.MAIN, listOf(NormalizedPoint(0.0, 1.0), NormalizedPoint(1.0, 0.0))),
            ),
            axisTicks = emptyList(), refBands = emptyList(), markers = emptyList(),
            stats = Stats(emptyList(), emptyList(), null),
        )
        val dualData = LineChartData(
            series = listOf(
                Series("pace", emptyList(), Axis.PRIMARY, SeriesRole.MAIN),
                Series("hr", emptyList(), Axis.SECONDARY, SeriesRole.MAIN),
            ),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )
        val layers = build(layout = dualLayout, data = dualData)
        assertNotNull(layers.named("series.gradient.pace"))
        assertNull(layers.named("series.gradient.hr"))
        assertNotNull(layers.named("series.main.hr"))
    }

    @Test
    fun mainLinePathSpansPlotRect() {
        val main = build().named("series.main.pace") as StrokeLayer
        // (0,0)→(0,100), (1,1)→(100,0)
        assertContentEquals(floatArrayOf(0f, 0f, 100f, 100f), main.bounds())
        assertEquals(style.lineWidth, main.width)
    }

    @Test
    fun ghostLineIsDashed() {
        val ghost = build().named("series.ghost.pace_prev") as StrokeLayer
        assertContentEquals(style.ghostDashPattern, ghost.dash)
    }

    @Test
    fun bandCoversNormalizedRange() {
        val band = build().named("band.0") as RectLayer
        // lower 0.25→y75, upper 0.75→y25 → (0,25,100,50)
        assertEquals(0.0, band.minX)
        assertEquals(25.0, band.minY)
        assertEquals(100.0, band.width)
        assertEquals(50.0, band.height)
    }

    @Test
    fun markersAreVerticalLinesWithLabels() {
        val layers = build()
        val marker = layers.named("marker.0") as ContainerLayer
        val line = marker.strokeChild()
        assertEquals(50f, line.bounds()[0]) // x = 50
        assertTrue(marker.hasText())
        val emphasis = (layers.named("marker.1") as ContainerLayer).strokeChild()
        assertEquals(1.5f, emphasis.width)
    }

    @Test
    fun axisLabelsUseFormatter() {
        val layers = build(formatter = { axis, value ->
            if (axis == ChartAxis.X) "${value.toInt()}km" else "v${value.toInt()}"
        })
        val xLabels = (layers.named("axisLabels.x") as ContainerLayer)
            .children.filterIsInstance<TextLayer>().map { it.text }
        assertEquals(listOf("0km", "5km"), xLabels)
        val yLabels = (layers.named("axisLabels.yPrimary") as ContainerLayer)
            .children.filterIsInstance<TextLayer>().map { it.text }
        assertEquals(listOf("v4", "v6"), yLabels)
    }

    @Test
    fun invertedAxisKeepsBandFrame() {
        val inverted = PlotArea(100.0, 100.0, Insets(0f, 0f, 0f, 0f), invertedAxes = setOf(Axis.PRIMARY))
        val band = build(plot = inverted).named("band.0") as RectLayer
        assertEquals(25.0, band.minY)
        assertEquals(50.0, band.height)
    }

    @Test
    fun notRenderablePlotAreaProducesNoLayers() {
        val zero = PlotArea(0.0, 0.0, Insets(0f, 0f, 0f, 0f))
        assertTrue(build(plot = zero).isEmpty())
    }

    @Test
    fun overlaySeriesProducesDashedLayerWithoutAxisLabelsOrGradient() {
        val overlayLayout = LineChartLayout(
            series = listOf(
                SeriesLayout("p", SeriesRole.MAIN, listOf(NormalizedPoint(0.0, 0.2), NormalizedPoint(1.0, 0.8))),
                SeriesLayout("o", SeriesRole.OVERLAY, listOf(NormalizedPoint(0.0, 0.0), NormalizedPoint(1.0, 1.0))),
            ),
            axisTicks = emptyList(), refBands = emptyList(), markers = emptyList(),
            stats = Stats(emptyList(), emptyList(), null),
        )
        val overlayData = LineChartData(
            series = listOf(
                Series("p", emptyList(), Axis.PRIMARY, SeriesRole.MAIN),
                Series("o", emptyList(), Axis.PRIMARY, SeriesRole.OVERLAY),
            ),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )
        val layers = build(layout = overlayLayout, data = overlayData)
        val names = layers.map { it.name }
        assertTrue(names.contains("series.overlay.o"))
        assertFalse(names.any { it.startsWith("series.gradient.o") })
        assertFalse(names.any { it.startsWith("axisLabels") })
        val overlay = layers.named("series.overlay.o") as StrokeLayer
        assertContentEquals(style.overlayLineDashPattern, overlay.dash)
        assertEquals(style.overlayLineColor, overlay.color)
        assertEquals(style.overlayLineWidth, overlay.width)
    }

    @Test
    fun overlaySeriesIgnoresHostAxisInversion() {
        val overlayLayout = LineChartLayout(
            series = listOf(
                SeriesLayout("o", SeriesRole.OVERLAY, listOf(NormalizedPoint(0.0, 0.0), NormalizedPoint(1.0, 1.0))),
            ),
            axisTicks = emptyList(), refBands = emptyList(), markers = emptyList(),
            stats = Stats(emptyList(), emptyList(), null),
        )
        val overlayData = LineChartData(
            series = listOf(Series("o", emptyList(), Axis.PRIMARY, SeriesRole.OVERLAY)),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )
        val inverted = PlotArea(100.0, 100.0, Insets(0f, 0f, 0f, 0f), invertedAxes = setOf(Axis.PRIMARY))
        val overlay = build(layout = overlayLayout, data = overlayData, plot = inverted)
            .named("series.overlay.o") as StrokeLayer
        val pts = overlay.segments.first()
        assertEquals(2, pts.size)
        // x=0→fraction0.0(최소), x=1→1.0(최대) → 최대값이 화면 위(작은 y)
        assertTrue(pts[1].y < pts[0].y)
    }

    @Test
    fun markerLabelsSitAbovePlotTopInset() {
        // Major-1 회귀 가드: 구간(km) 마커 라벨은 플롯 상단 여백(plot.minY 위)에 그려진다.
        // 그러므로 clipRect의 top이 plot.minY이면 이 라벨들이 전부 잘려 화면에서 사라진다 —
        // drawLineChart는 iOS 확대 마스크(y=0)와 동치로 top을 0f까지 열어야 한다.
        val insetPlot = PlotArea(120.0, 120.0, Insets(top = 16f, left = 8f, bottom = 20f, right = 8f))
        val layers = build(plot = insetPlot)

        val markerLabel = (layers.named("marker.0") as ContainerLayer)
            .children.filterIsInstance<TextLayer>().first()
        assertEquals(VAlign.ABOVE, markerLabel.vAlign)
        assertTrue(
            markerLabel.anchorY < insetPlot.minY,
            "마커 라벨 앵커(${markerLabel.anchorY})는 플롯 상단(${insetPlot.minY}) 위여야 한다",
        )
    }

    @Test
    fun barCornerRadiusCarriedOnRectLayerAndBandIsSquare() {
        // Minor-1: 바 RectLayer는 style.barCornerRadius(>0)를 실어야 하고(둥근 모서리),
        // 밴드 RectLayer는 cornerRadius=0(직각)이어야 한다.
        assertTrue(style.barCornerRadius > 0f)
        val band = build().named("band.0") as RectLayer
        assertEquals(0f, band.cornerRadius)
    }

    @Test
    fun axisLabelFontFamilyAndWeightPropagateToAllLabelLayers() {
        // QA Minor-6: iOS axisLabelFont(UIFont — 패밀리/웨이트 주입 가능)는 축·마커 라벨 전부에
        // 쓰인다. Android도 ChartStyle 주입 폰트가 모든 라벨 TextLayer에 실려야 한다(기본 null=시스템).
        val customStyle = style.copy(
            axisLabelFontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            axisLabelFontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
        val layers = build(style = customStyle)
        val labels = listOf(
            (layers.named("axisLabels.x") as ContainerLayer).children.filterIsInstance<TextLayer>().first(),
            (layers.named("marker.0") as ContainerLayer).children.filterIsInstance<TextLayer>().first(),
        )
        labels.forEach { label ->
            assertEquals(androidx.compose.ui.text.font.FontFamily.Monospace, label.fontFamily, label.name)
            assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, label.fontWeight, label.name)
        }
        // 기본 스타일은 시스템 폰트(null) — 주입 전 동작 보존.
        val defaultLabel = (build().named("axisLabels.x") as ContainerLayer)
            .children.filterIsInstance<TextLayer>().first()
        assertNull(defaultLabel.fontFamily)
        assertNull(defaultLabel.fontWeight)
    }

    @Test
    fun seriesWithFewerThanTwoPointsIsSkipped() {
        val single = LineChartLayout(
            series = listOf(SeriesLayout("one", SeriesRole.MAIN, listOf(NormalizedPoint(0.5, 0.5)))),
            axisTicks = emptyList(), refBands = emptyList(), markers = emptyList(),
            stats = Stats(emptyList(), emptyList(), null),
        )
        assertTrue(build(layout = single).isEmpty())
    }
}
