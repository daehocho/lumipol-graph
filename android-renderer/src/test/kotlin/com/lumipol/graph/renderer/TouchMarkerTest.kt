package com.lumipol.graph.renderer

import com.lumipol.graph.LineChartEngine
import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.ChartConfig
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.LineChartLayout
import com.lumipol.graph.model.Point
import com.lumipol.graph.model.Series
import com.lumipol.graph.model.SeriesRole
import com.lumipol.graph.model.Stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// iOS: TouchMarkerTests.swift — 마커 조립 순수부 검증(엔진 nearest 통과 실제 레이아웃 사용).
class TouchMarkerTest {

    private val style = ChartStyle.defaults(darkTheme = false)
    private val data = TestFixtures.fullChart
    private val layout: LineChartLayout get() = LineChartEngine.layout(data)
    private val plot = PlotArea(390.0, 300.0, style.plotInsets, invertedAxes = setOf(Axis.PRIMARY))

    private fun makeResult(rawX: Double, layout: LineChartLayout = this.layout, data: LineChartData = this.data) =
        TouchMarker.make(
            rawX,
            TouchMarkerContext(data, layout, style, plot, TestFixtures::format),
        )

    private fun childNames(result: TouchMarkerResult) = result.layer.children.map { it.name }

    @Test
    fun markerContainsLineAndDotsButNoBubble() {
        val result = makeResult(2.4)!!
        assertEquals("touch.marker", result.layer.name)
        val names = childNames(result)
        assertTrue(names.contains("touch.line"))
        assertTrue(names.contains("touch.dot.pace"))
        assertTrue(names.contains("touch.dot.pace_prev"))
        assertTrue(names.contains("touch.dot.hr"))
        assertFalse(names.contains("touch.bubble"))
    }

    @Test
    fun returnsFormattedValuesPerSeries() {
        // rawX 2.4 → 근접점 x=2.5 (인덱스 5): pace 5.5 → "5'30\"", hr 166 → "166"
        val values = makeResult(2.4)!!.valuesBySeriesId
        assertEquals("5'30\"", values["pace"])
        assertEquals("166", values["hr"])
    }

    @Test
    fun dotSitsOnSnappedX() {
        val result = makeResult(0.0)!!
        val line = result.layer.children.first { it.name == "touch.line" } as StrokeLayer
        val dot = result.layer.children.first { it.name == "touch.dot.pace" } as DotLayer
        assertEquals(line.segments.first().first().x, dot.center.x, 0.5)
    }

    @Test
    fun markerShownAtDomainUpperBound() {
        // 도메인 상한(x=5.0) — 정규화 x가 반올림으로 1을 수 ulp 넘어도 침묵 드롭 금지.
        val result = makeResult(5.0)
        assertNotNull(result)
        assertEquals("touch.marker", result.layer.name)
    }

    @Test
    fun overlaySeriesValueUsesRealValueAndOverlayAxis() {
        val overlayData = LineChartData(
            series = listOf(
                TestFixtures.series("pace", TestFixtures.paceValues, Axis.PRIMARY, SeriesRole.MAIN),
                Series("o", listOf(Point(0.0, 1500.0), Point(5.0, 1500.0)), Axis.PRIMARY, SeriesRole.OVERLAY),
            ),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )
        val overlayLayout = LineChartEngine.layout(overlayData)
        var seenOverlayAxis = false
        val result = TouchMarker.make(
            0.0,
            TouchMarkerContext(overlayData, overlayLayout, style, plot, formatter = { axis, value ->
                if (axis == ChartAxis.Y_OVERLAY) {
                    seenOverlayAxis = true
                    "OV:${value.toInt()}"
                } else {
                    TestFixtures.format(axis, value)
                }
            }),
        )
        assertEquals("OV:1500", result!!.valuesBySeriesId["o"])
        assertTrue(seenOverlayAxis)
        assertFalse(childNames(result).contains("touch.dot.o"))
    }

    @Test
    fun snappedXPrefersMainSeriesWhenGhostListedFirst() {
        val ghostFirst = LineChartData(
            series = listOf(
                Series("prev", listOf(Point(0.0, 6.0), Point(2.0, 6.2)), Axis.PRIMARY, SeriesRole.GHOST),
                TestFixtures.series("pace", TestFixtures.paceValues, Axis.PRIMARY, SeriesRole.MAIN),
            ),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )
        val result = makeResult(1.3, LineChartEngine.layout(ghostFirst), ghostFirst)
        // main(0.5 간격) 근접점 = 1.5. 고스트 근접점(2.0)이 기준이 되면 안 된다.
        assertEquals(1.5, result!!.snappedX, 1e-9)
    }

    @Test
    fun outOfWindowSeriesOmittedFromDotsAndValues() {
        val ghostShort = LineChartData(
            series = listOf(
                TestFixtures.series("pace", TestFixtures.paceValues, Axis.PRIMARY, SeriesRole.MAIN),
                Series("prev", listOf(Point(0.0, 6.0), Point(2.0, 6.2)), Axis.PRIMARY, SeriesRole.GHOST),
            ),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )
        val windowed = LineChartEngine.layout(ghostShort, 3.0, 5.0)
        val result = makeResult(4.0, windowed, ghostShort)
        assertNotNull(result)
        assertNotNull(result.valuesBySeriesId["pace"])
        assertNull(result.valuesBySeriesId["prev"])
        assertFalse(childNames(result).contains("touch.dot.prev"))
    }

    @Test
    fun zoomedEdgeSnapsToInWindowPointWhenGlobalNearestIsOutsideWindow() {
        // 창 [0,5], main에 4.4(안)·5.3(밖): 오른쪽 끝 스크럽(rawX=5)의 전역 최근접은 5.3이지만
        // 창 밖이라며 마커 전체를 버리면 안 되고, 창 안 4.4에 스냅해야 한다.
        val d = LineChartData(
            series = listOf(
                Series("pace", listOf(Point(0.0, 6.0), Point(4.4, 5.5), Point(5.3, 5.2)), Axis.PRIMARY, SeriesRole.MAIN),
            ),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )
        val windowed = LineChartEngine.layout(d, 0.0, 5.0)
        val result = makeResult(5.0, windowed, d)
        assertNotNull(result)
        assertEquals(4.4, result.snappedX, 1e-9)
    }

    @Test
    fun duplicateSeriesIdsDoNotCrash() {
        val dupData = LineChartData(
            series = listOf(
                TestFixtures.series("pace", TestFixtures.paceValues, Axis.PRIMARY, SeriesRole.MAIN),
                TestFixtures.series("pace", TestFixtures.ghostPaceValues, Axis.PRIMARY, SeriesRole.GHOST),
            ),
            config = ChartConfig(segmentCount = 0, maxTicks = 5),
        )
        assertNotNull(makeResult(2.4, LineChartEngine.layout(dupData), dupData))
    }

    @Test
    fun returnsNilWhenAxisScaleUnavailable() {
        val emptyLayout = LineChartLayout(
            series = emptyList(), axisTicks = emptyList(), refLines = emptyList(),
            refBands = emptyList(), markers = emptyList(),
            stats = Stats(emptyList(), emptyList(), null),
        )
        assertNull(makeResult(1.0, emptyLayout))
    }
}
