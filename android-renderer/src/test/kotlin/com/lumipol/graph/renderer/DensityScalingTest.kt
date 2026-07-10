package com.lumipol.graph.renderer

import com.lumipol.graph.DonutEngine
import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutColorRole
import com.lumipol.graph.model.DonutSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * UX Critical-1 / Arch m4 회귀 방지: [scaledForDensity]가 기하 값을 dp→px로 정확히 곱하고,
 * 색·알파·비율·sp는 보존하며, 헤어라인 하한을 적용하는지 순수 JVM으로 검증한다.
 * (iOS 패리티 수치는 density=1에서 그대로이므로 기존 96개 테스트는 원본 스타일을 계속 사용한다.)
 */
class DensityScalingTest {

    private val base = ChartStyle.defaults(darkTheme = false)

    @Test
    fun geometryScalesByDensity() {
        val s = base.scaledForDensity(3f)
        assertEquals(base.lineWidth * 3f, s.lineWidth)
        assertEquals(base.ghostLineWidth * 3f, s.ghostLineWidth)
        assertEquals(base.overlayLineWidth * 3f, s.overlayLineWidth)
        assertEquals(base.donutRingWidth * 3f, s.donutRingWidth)
        assertEquals(base.touchDotRadius * 3f, s.touchDotRadius)
        assertEquals(base.barCornerRadius * 3f, s.barCornerRadius)
        assertEquals(base.barMinHeight * 3f, s.barMinHeight)
        assertEquals(base.plotInsets.left * 3f, s.plotInsets.left)
        assertEquals(base.plotInsets.top * 3f, s.plotInsets.top)
        // dash 패턴도 스케일
        assertEquals(base.gridLineDashPattern[0] * 3f, s.gridLineDashPattern[0])
        assertEquals(base.ghostDashPattern[0] * 3f, s.ghostDashPattern[0])
    }

    @Test
    fun nonGeometryValuesPreserved() {
        val s = base.scaledForDensity(3f)
        // 색·알파·비율·sp는 밀도와 무관 — 그대로 유지.
        assertEquals(base.primaryLineColor, s.primaryLineColor)
        assertEquals(base.gradientMaxAlpha, s.gradientMaxAlpha)
        assertEquals(base.areaHeightFraction, s.areaHeightFraction)
        assertEquals(base.axisLabelFontSize, s.axisLabelFontSize)
        assertEquals(base.donutColors, s.donutColors)
    }

    @Test
    fun density1IsIdentityExceptHairlineFloor() {
        val s = base.scaledForDensity(1f)
        assertEquals(base.lineWidth, s.lineWidth)
        assertEquals(base.plotInsets.left, s.plotInsets.left)
        // gridLineWidth 0.5dp는 1x에서 서브픽셀 소실 방지를 위해 1px로 올린다(헤어라인 하한).
        assertEquals(ChartStyle.HAIRLINE_MIN_PX, s.gridLineWidth)
    }

    @Test
    fun hairlineFloorAppliedAtLowDensity() {
        // gridLineWidth 0.5dp * 1.5 = 0.75px < 1px → 하한 1px.
        assertEquals(ChartStyle.HAIRLINE_MIN_PX, base.scaledForDensity(1.5f).gridLineWidth)
        // 3x에서는 0.5*3=1.5px로 하한을 넘음.
        assertEquals(0.5f * 3f, base.scaledForDensity(3f).gridLineWidth)
    }

    @Test
    fun widenedHitBandSelectsThinRingBeyondVisualBand() {
        // UX Major-3: 얇은 링(20px)에서 시각 대역 밖이라도 넓힌 히트 대역(48px) 안이면 탭 성공.
        val data = DonutChartData(listOf(DonutSegment(100.0, DonutColorRole.ZONE1)))
        val layout = DonutEngine.layout(data)
        val w = 200f
        val h = 200f
        val ring = 20f
        // 반경 = (200-20)/2 = 90. 12시 방향으로 중심에서 거리 70인 점(시각 대역 [80,100] 밖).
        val px = w / 2f
        val py = h / 2f - 70f

        // 기본(대역 = 링 20px → [80,100]): 거리 70은 대역 밖 → null.
        assertNull(donutSegmentIndex(px, py, w, h, ring, layout))
        // 넓힌 대역 48px([66,114]): 거리 70은 대역 안 → 세그먼트 0 선택.
        assertEquals(0, donutSegmentIndex(px, py, w, h, ring, layout, hitBandWidth = 48f))
    }

    @Test
    fun widenedHitBandStillRejectsHoleAndOutside() {
        val data = DonutChartData(listOf(DonutSegment(100.0, DonutColorRole.ZONE1)))
        val layout = DonutEngine.layout(data)
        // 구멍 중앙(거리 0)·링 밖 먼 점은 넓힌 대역에서도 여전히 미선택.
        assertNull(donutSegmentIndex(100f, 100f, 200f, 200f, 20f, layout, hitBandWidth = 48f))
        assertNull(donutSegmentIndex(5f, 5f, 200f, 200f, 20f, layout, hitBandWidth = 48f))
    }

    @Test
    fun buildersRemainStableAtDensity1() {
        // density 파라미터 기본값(1f)에서 도넛 arc는 원본과 동일(기존 테스트 수치 보존).
        val data = DonutChartData(listOf(DonutSegment(100.0, DonutColorRole.ZONE4)))
        val arcs = buildDonutArcs(DonutEngine.layout(data), base, 200f, 200f)
        assertNotNull(arcs.firstOrNull())
        assertEquals(base.donutRingWidth, arcs.first().strokeWidth)
    }
}
