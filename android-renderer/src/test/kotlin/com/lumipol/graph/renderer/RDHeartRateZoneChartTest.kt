package com.lumipol.graph.renderer

import com.lumipol.graph.DonutEngine
import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutColorRole
import com.lumipol.graph.model.DonutSegment
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// iOS: HeartRateZoneViewTests.swift — arc 조립 + 히트테스트 순수부 검증.
class RDHeartRateZoneChartTest {

    private val style = ChartStyle.defaults(darkTheme = false)
    private val width = 200f
    private val height = 200f

    private fun arcs(data: DonutChartData) =
        buildDonutArcs(DonutEngine.layout(data), style, width, height)

    /** 링 중심선 위의 점(frac=0=12시, 시계방향). iOS ringPoint 대응. */
    private fun ringPoint(frac: Float): Pair<Float, Float> {
        val ring = style.donutRingWidth
        val radius = (min(width, height) - ring) / 2f
        val angle = frac * 2f * Math.PI.toFloat() - (Math.PI / 2).toFloat()
        return (width / 2f + radius * cos(angle)) to (height / 2f + radius * sin(angle))
    }

    private fun indexAt(data: DonutChartData, px: Float, py: Float): Int? =
        donutSegmentIndex(px, py, width, height, style.donutRingWidth, DonutEngine.layout(data))

    @Test
    fun rendersOneArcPerSegment() {
        val arcs = arcs(
            DonutChartData(
                listOf(
                    DonutSegment(30.0, DonutColorRole.ZONE1),
                    DonutSegment(40.0, DonutColorRole.ZONE3),
                    DonutSegment(30.0, DonutColorRole.ZONE5),
                ),
            ),
        )
        assertEquals(3, arcs.size)
    }

    @Test
    fun emptyDataRendersSingleGrayRing() {
        val arcs = arcs(DonutChartData(emptyList()))
        assertEquals(1, arcs.size)
        assertEquals(style.donutEmptyColor, arcs[0].color)
    }

    @Test
    fun segmentUsesStyleColor() {
        val arcs = arcs(DonutChartData(listOf(DonutSegment(100.0, DonutColorRole.ZONE4))))
        assertEquals(style.donutColors[DonutColorRole.ZONE4], arcs[0].color)
    }

    @Test
    fun missingDonutRoleFallsBackToStyleFallbackColor() {
        // 색 role이 주입 맵에 없으면 하드코딩 회색이 아니라 ChartStyle.fallbackDataColor를 쓴다.
        val custom = style.copy(donutColors = emptyMap(), fallbackDataColor = androidx.compose.ui.graphics.Color.Magenta)
        val arcs = buildDonutArcs(
            DonutEngine.layout(DonutChartData(listOf(DonutSegment(100.0, DonutColorRole.ZONE4)))),
            custom, width, height,
        )
        assertEquals(androidx.compose.ui.graphics.Color.Magenta, arcs[0].color)
    }

    @Test
    fun segmentIndexMapsToOriginalDataIndexWhenZeroValueSegmentsFiltered() {
        val data = DonutChartData(
            listOf(
                DonutSegment(0.0, DonutColorRole.ZONE1),
                DonutSegment(30.0, DonutColorRole.ZONE2),
                DonutSegment(70.0, DonutColorRole.ZONE3),
            ),
        )
        // 첫 호(0~30%) = zone2 → 원본 인덱스 1
        val p1 = ringPoint(0.15f)
        assertEquals(1, indexAt(data, p1.first, p1.second))
        // 두 번째 호(30~100%) = zone3 → 원본 인덱스 2
        val p2 = ringPoint(0.6f)
        assertEquals(2, indexAt(data, p2.first, p2.second))
    }

    @Test
    fun touchInDonutHoleOrOutsideRingSelectsNothing() {
        val data = DonutChartData(
            listOf(
                DonutSegment(30.0, DonutColorRole.ZONE1),
                DonutSegment(70.0, DonutColorRole.ZONE3),
            ),
        )
        assertNull(indexAt(data, 100f, 105f)) // 구멍 중앙
        assertNull(indexAt(data, 5f, 5f))     // 링 밖 모서리
        val onRing = ringPoint(0.15f)
        assertEquals(0, indexAt(data, onRing.first, onRing.second)) // 링 위는 선택됨
    }

    @Test
    fun tinyBoundsSkipsDrawingInsteadOfNegativeRadius() {
        // ring(28)보다 작은 bounds → radius 음수 → 빈 리스트(크래시/쓰레기 없음).
        val arcs = buildDonutArcs(
            DonutEngine.layout(
                DonutChartData(
                    listOf(
                        DonutSegment(30.0, DonutColorRole.ZONE1),
                        DonutSegment(70.0, DonutColorRole.ZONE3),
                    ),
                ),
            ),
            style, 10f, 10f,
        )
        assertEquals(0, arcs.size)
    }
}
