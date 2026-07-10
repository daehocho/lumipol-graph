package com.lumipol.graph.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.AxisTick
import com.lumipol.graph.model.AxisTicksLayout
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.ChartConfig
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.LineChartLayout
import com.lumipol.graph.model.MarkerLayout
import com.lumipol.graph.model.NormalizedPoint
import com.lumipol.graph.model.Series
import com.lumipol.graph.model.SeriesLayout
import com.lumipol.graph.model.SeriesRole
import com.lumipol.graph.model.Stats
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * DrawScope 픽셀 렌더 검증(Robolectric NATIVE 그래픽스) — 중간 모델 테스트가 닿지 못한 실제 그리기
 * 계층의 두 회귀를 고정한다: (Major-1) 상단 여백 마커 라벨이 클립되지 않고 그려짐,
 * (Minor-1) 바 RectLayer가 drawRoundRect로 둥근 모서리를 그림.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LineChartRenderTest {

    private val density = Density(1f)
    private val measurer = TextMeasurer(
        createFontFamilyResolver(ApplicationProvider.getApplicationContext()),
        density,
        LayoutDirection.Ltr,
    )
    private val style = ChartStyle.defaults(darkTheme = false)

    /** [block]을 지정 크기의 투명 비트맵에 [CanvasDrawScope]로 렌더하고 픽셀맵을 돌려준다. */
    private fun renderToPixels(
        width: Int,
        height: Int,
        block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
    ) = ImageBitmap(width, height).also { bitmap ->
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat()), block)
    }.toPixelMap()

    @Test
    fun markerLabelIsNotClippedAbovePlotTop() {
        val topInset = 40f
        val plot = PlotArea(200.0, 200.0, Insets(top = topInset, left = 10f, bottom = 20f, right = 10f))
        val layout = LineChartLayout(
            series = listOf(
                SeriesLayout("pace", SeriesRole.MAIN, listOf(NormalizedPoint(0.0, 0.2), NormalizedPoint(1.0, 0.8))),
            ),
            axisTicks = listOf(AxisTicksLayout(ChartAxis.X, listOf(AxisTick(0.0, 0.0), AxisTick(5.0, 1.0)))),
            refLines = emptyList(),
            refBands = emptyList(),
            markers = listOf(MarkerLayout(0.5, "10km", false)),
            stats = Stats(emptyList(), emptyList(), null),
        )
        val data = LineChartData(
            series = listOf(Series("pace", emptyList(), Axis.PRIMARY, SeriesRole.MAIN)),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )

        val pixels = renderToPixels(200, 200) {
            drawLineChart(layout, data, style, plot, { _, v -> "$v" }, measurer)
        }

        // 플롯 상단 여백([0, topInset))의 플롯 x범위에서 마커 라벨 픽셀이 존재해야 한다.
        // 이전 clip(top=plot.minY)에서는 이 영역이 전부 잘려 0이었다.
        var painted = 0
        for (y in 0 until topInset.toInt()) {
            for (x in plot.minX.toInt() until plot.maxX.toInt()) {
                if (pixels[x, y].alpha > 0f) painted++
            }
        }
        assertTrue(painted > 0, "상단 여백에 마커 라벨이 렌더되지 않았다(클립됨)")
    }

    @Test
    fun rectLayerWithCornerRadiusRoundsCorners() {
        // 40x40 사각형에 반지름 20(=반폭) → 실질적으로 원. 바운딩박스 모서리는 투명(둥긂), 중앙은 채워짐.
        val rounded = RectLayer(
            name = "bar.0",
            minX = 10.0, minY = 10.0, width = 40.0, height = 40.0,
            color = Color.Red, cornerRadius = 20f,
        )
        val pixels = renderToPixels(60, 60) { render(rounded, measurer) }
        assertTrue(pixels[30, 30].alpha > 0f, "중앙은 채워져야 한다")
        assertTrue(pixels[11, 11].alpha == 0f, "둥근 모서리 바깥(11,11)은 투명해야 한다")

        // 대조군: cornerRadius=0이면 같은 모서리가 채워진다(직각).
        val square = rounded.copy(name = "band.0", cornerRadius = 0f)
        val squarePixels = renderToPixels(60, 60) { render(square, measurer) }
        assertTrue(squarePixels[11, 11].alpha > 0f, "직각 사각형 모서리(11,11)는 채워져야 한다")
    }
}
