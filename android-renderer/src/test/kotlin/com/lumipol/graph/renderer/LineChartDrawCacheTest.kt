package com.lumipol.graph.renderer

import com.lumipol.graph.LineChartEngine
import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.ChartAxis
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 스크럽 프레임(마커만 변함) 캐시 계약: 입력이 같으면 레이어 리스트·Path 인스턴스를 재사용하고,
 * 입력(layout/plot/style/area)이 바뀌면 재조립한다 — 60~120Hz 스크럽에서 전체 재계산 방지.
 * (Path 생성은 android.graphics 의존 → Robolectric NATIVE.)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LineChartDrawCacheTest {

    private val style = ChartStyle.defaults(darkTheme = false)
    private val data = TestFixtures.paceAndHeartRate
    private val layout = LineChartEngine.layout(data)
    private val plot = PlotArea(390.0, 300.0, style.plotInsets, invertedAxes = setOf(Axis.PRIMARY))
    private val formatter = TestFixtures::format
    private val area = TestFixtures.area(0.0 to 0.0, 5.0 to 100.0)

    private fun update(cache: LineChartDrawCache, l: com.lumipol.graph.model.LineChartLayout = layout, p: PlotArea = plot) =
        cache.update(l, data, style, p, formatter, density = 1f, sortedArea = area)

    @Test
    fun sameInputsReuseLayerInstances() {
        val cache = LineChartDrawCache()
        assertTrue(update(cache), "최초 갱신은 재조립")
        val clipped = cache.clipped
        val labels = cache.axisLabels
        val background = cache.background
        assertTrue(!update(cache), "같은 입력이면 재조립하지 않아야 함")
        assertSame(clipped, cache.clipped)
        assertSame(labels, cache.axisLabels)
        assertSame(background, cache.background)
    }

    @Test
    fun strokePathIsReusedAcrossFrames() {
        val cache = LineChartDrawCache()
        update(cache)
        val stroke = cache.clipped.filterIsInstance<StrokeLayer>().first()
        assertSame(cache.strokePath(stroke), cache.strokePath(stroke), "같은 레이어는 같은 Path 인스턴스")
    }

    @Test
    fun layoutChangeRebuildsLayersAndPaths() {
        val cache = LineChartDrawCache()
        update(cache)
        val strokeBefore = cache.clipped.filterIsInstance<StrokeLayer>().first()
        val pathBefore = cache.strokePath(strokeBefore)
        val zoomed = LineChartEngine.layout(data, 1.0, 3.0)
        assertTrue(update(cache, l = zoomed), "layout이 바뀌면 재조립")
        val strokeAfter = cache.clipped.filterIsInstance<StrokeLayer>().first()
        assertNotSame(pathBefore, cache.strokePath(strokeAfter))
    }

    @Test
    fun backgroundSilhouetteIsBuiltFromAreaAndReused() {
        val cache = LineChartDrawCache()
        update(cache)
        val bg = cache.background
        assertNotNull(bg, "sortedArea가 있으면 실루엣 캐시")
        update(cache)
        assertSame(bg, cache.background)
    }

    @Test
    fun plotResizeRebuilds() {
        val cache = LineChartDrawCache()
        update(cache)
        val before = cache.clipped
        assertTrue(update(cache, p = PlotArea(500.0, 300.0, style.plotInsets, invertedAxes = setOf(Axis.PRIMARY))))
        assertNotSame(before, cache.clipped)
    }

    @Test
    fun axisLabelsArePartitionedOut() {
        val cache = LineChartDrawCache()
        update(cache)
        assertTrue(cache.axisLabels.isNotEmpty())
        assertTrue(cache.axisLabels.all { it.name.startsWith("axisLabels.") })
        assertTrue(cache.clipped.none { it.name.startsWith("axisLabels.") })
        // X축 라벨 존재 확인(조립 자체 검증은 LineChartDrawingTest 소관).
        assertTrue(cache.axisLabels.any { (it as ContainerLayer).name == "axisLabels.x" })
        assertTrue(layout.axisTicks.any { it.axis == ChartAxis.X })
    }
}
