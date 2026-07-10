package com.lumipol.graph.renderer

import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * iOS: RDChartViewTests(24) + ZoomInteractionTests(14) 중 **상호작용 홀더 로직**을 이식.
 *
 * iOS는 `RDChartView`(UIView) 전체를 layout까지 돌려 검증했지만, Compose에선 상호작용 로직이
 * [LineChartInteraction]에 캡슐화돼 순수 JVM으로 검증 가능하다(레이어 트리 조립은 `LineChartDrawingTest`,
 * 컴포저블 배선·제스처는 Robolectric/compose-ui-test). "마커 표시 여부"는 iOS의 `touch.marker` 레이어
 * 존재 = Compose에선 [LineChartInteraction.activeMarkerRawX] != null(성공한 scrubTo에서만 세팅)로 매핑.
 */
class LineChartInteractionTest {

    private val style = ChartStyle.defaults(darkTheme = false)
    private val inverted = setOf(Axis.PRIMARY)

    private class Spy {
        val scrubbed = mutableListOf<Map<String, String>>()
        var endCount = 0
        val backgroundValues = mutableListOf<Double>()
    }

    private fun holder(data: LineChartData, area: List<Point>? = null): Pair<LineChartInteraction, Spy> {
        val sorted = area?.sortedBy { it.x }
        val h = LineChartInteraction(data, sorted)
        val spy = Spy()
        h.onScrub = { spy.scrubbed.add(it) }
        h.onScrubBackground = { spy.backgroundValues.add(it) }
        h.onScrubEnd = { spy.endCount++ }
        return h to spy
    }

    /** 현재 layout·plot으로 마커 컨텍스트 조립(그리기와 동일 좌표계, 390×300 뷰). */
    private fun ctx(h: LineChartInteraction): TouchMarkerContext {
        val plot = PlotArea(390.0, 300.0, style.plotInsets, inverted)
        return TouchMarkerContext(h.data, h.layoutForCurrentWindow(), style, plot, TestFixtures::format)
    }

    /** iOS `showTouchMarker(atX:)` = notify=true. */
    private fun show(h: LineChartInteraction, rawX: Double): Boolean =
        h.scrubTo(rawX, ctx(h), notify = true)

    private fun xTicks(h: LineChartInteraction): List<Double> =
        h.layoutForCurrentWindow().axisTicks.first { it.axis == ChartAxis.X }.ticks.map { it.value }

    // ------------------------------------------------------------------ 스크럽 델리게이트

    @Test
    fun showTouchMarkerReportsValuesToDelegate() {
        val (h, spy) = holder(TestFixtures.fullChart)
        assertTrue(show(h, 2.4))
        assertEquals("5'30\"", spy.scrubbed.last()["pace"])
        assertEquals("166", spy.scrubbed.last()["hr"])
        assertEquals(0, spy.endCount, "표시 중에는 종료 콜백 없음")
        assertEquals(2.4, h.activeMarkerRawX)
    }

    @Test
    fun relayoutRestoresMarkerWithoutRefiringScrubDelegate() {
        val (h, spy) = holder(TestFixtures.fullChart, TestFixtures.area(0.0 to 0.0, 5.0 to 100.0))
        assertTrue(show(h, 2.4))
        assertEquals(1, spy.scrubbed.size)
        assertEquals(1, spy.backgroundValues.size)
        // relayout 복원 경로(notify=false) — 콜백 재발화 금지, 마커 유지.
        assertTrue(h.scrubTo(2.4, ctx(h), notify = false))
        assertEquals(1, spy.scrubbed.size, "레이아웃 패스는 didScrubTo 재발화 안 함")
        assertEquals(1, spy.backgroundValues.size, "레이아웃 패스는 배경 값 콜백 재발화 안 함")
        assertEquals(2.4, h.activeMarkerRawX, "마커는 복원")
    }

    @Test
    fun hideTouchMarkerReportsEndOnceWhenShown() {
        val (h, spy) = holder(TestFixtures.fullChart)
        show(h, 2.4)
        h.endScrub()
        assertEquals(1, spy.endCount)
        assertNull(h.activeMarkerRawX)
    }

    @Test
    fun noEndScrubWhenMarkerCannotBeMadeAndNothingWasShowing() {
        val (h, spy) = holder(TestFixtures.fullChart)
        // 포인트 간격(0.5) 사이 갭으로 줌 — 창 안에 스냅할 점이 없어 마커 생성 실패.
        h.zoomToRange(1.6..1.9)
        assertFalse(show(h, 1.7))
        assertEquals(0, spy.scrubbed.size)
        assertEquals(0, spy.endCount, "표시된 마커가 없으면 종료 콜백도 없음")
    }

    @Test
    fun zoomToFullDomainRangeNormalizesToNotZoomed() {
        // 전체 도메인 줌 요청은 '비줌'(zoom == null)으로 정규화 — zoomed-but-full 상태가 남으면
        // '비줌 = null' 불변식이 깨져 읽는 쪽마다 이중 가드가 필요해진다.
        val (h, _) = holder(TestFixtures.fullChart)
        h.zoomToRange(0.0..5.0)
        assertNull(h.zoom, "전체 도메인 창은 null로 정규화되어야 함")
    }

    @Test
    fun hideTouchMarkerNoEndWhenNothingShown() {
        val (h, spy) = holder(TestFixtures.fullChart)
        h.endScrub()
        assertEquals(0, spy.endCount)
    }

    // ------------------------------------------------------------------ 배경 area

    @Test
    fun scrubReportsBackgroundValueWhenAreaPresent() {
        val (h, spy) = holder(TestFixtures.fullChart, TestFixtures.area(0.0 to 0.0, 5.0 to 100.0))
        show(h, 2.4) // 근접점 스냅 x=2.5 → 보간값 50
        assertEquals(50.0, spy.backgroundValues.last(), 1e-9)
    }

    @Test
    fun scrubReportsCorrectBackgroundValueWhenAreaPointsUnsorted() {
        // 내림차순 입력이라도 홀더는 정렬본(sortedArea)을 받으므로 값이 올바라야 한다.
        val (h, spy) = holder(TestFixtures.fullChart, TestFixtures.area(5.0 to 100.0, 0.0 to 0.0))
        show(h, 2.4)
        assertEquals(50.0, spy.backgroundValues.last(), 1e-9)
    }

    @Test
    fun scrubOmitsBackgroundValueWhenNoArea() {
        val (h, spy) = holder(TestFixtures.fullChart)
        show(h, 2.4)
        assertFalse(spy.scrubbed.isEmpty(), "라인 값은 여전히 전달")
        assertTrue(spy.backgroundValues.isEmpty(), "area 없으면 배경 콜백 없음")
    }

    // ------------------------------------------------------------------ 배경 area 단독(시리즈 없음)

    @Test
    fun backgroundOnlyScrubShowsMarkerAndReportsInterpolatedValue() {
        val (h, spy) = holder(TestFixtures.emptySeries, TestFixtures.area(0.0 to 0.0, 10.0 to 100.0))
        // x=5 → 보간 50. area 범위(0~10)로 windowed layout돼야 함(0~1 붕괴 방지).
        assertTrue(show(h, 5.0))
        assertEquals(1, spy.scrubbed.size)
        assertEquals(emptyMap(), spy.scrubbed.last(), "라인 시리즈 없으니 빈 딕셔너리")
        assertEquals(50.0, spy.backgroundValues.last(), 1e-9)
        h.endScrub()
        assertEquals(1, spy.endCount)
    }

    @Test
    fun backgroundOnlyScrubFollowsRawXWithoutSnapping() {
        val (h, spy) = holder(TestFixtures.emptySeries, TestFixtures.area(0.0 to 0.0, 10.0 to 100.0))
        show(h, 2.5) // 스냅 격자 없음 → 연속 보간
        assertEquals(25.0, spy.backgroundValues.last(), 1e-9)
        show(h, 9.9)
        assertEquals(99.0, spy.backgroundValues.last(), 1e-9)
    }

    @Test
    fun backgroundOnlyWithoutAreaMakesNoMarker() {
        val (h, spy) = holder(TestFixtures.emptySeries)
        assertFalse(show(h, 0.5))
        assertNull(h.activeMarkerRawX)
        assertEquals(0, spy.scrubbed.size)
        assertTrue(spy.backgroundValues.isEmpty())
    }

    @Test
    fun seriesSnapFailureDoesNotFallBackToBackgroundMarker() {
        // 시리즈가 있으면 area가 있어도 스냅 실패 시 배경 단독 마커로 폴백하지 않는다.
        // 포인트 간격(0.5) 사이 갭으로 줌해 창 안에 스냅할 점이 없는 상황을 만든다.
        val (h, spy) = holder(TestFixtures.fullChart, TestFixtures.area(0.0 to 0.0, 5.0 to 100.0))
        h.zoomToRange(1.6..1.9)
        assertFalse(show(h, 1.7))
        assertEquals(0, spy.scrubbed.size)
        assertTrue(spy.backgroundValues.isEmpty())
    }

    @Test
    fun backgroundOnlyZoomResetKeepsAreaXDomain() {
        val (h, spy) = holder(TestFixtures.emptySeries, TestFixtures.area(0.0 to 0.0, 10.0 to 100.0))
        h.zoomToRange(2.0..6.0)
        h.resetZoom()
        assertEquals(50.0, run { show(h, 5.0); spy.backgroundValues.last() }, 1e-9)
    }

    // ------------------------------------------------------------------ 줌 상호작용

    @Test
    fun zoomToRangeRelayoutsTicksWithinWindow() {
        val (h, _) = holder(TestFixtures.paceAndHeartRate)
        val full = xTicks(h)
        h.zoomToRange(1.0..3.0)
        val zoomed = xTicks(h)
        assertNotEquals(full, zoomed)
        zoomed.forEach { assertTrue(it in 1.0..3.0) }
    }

    @Test
    fun resetZoomRestoresFullLayout() {
        val (h, _) = holder(TestFixtures.paceAndHeartRate)
        val full = xTicks(h)
        h.zoomToRange(1.0..3.0)
        h.resetZoom()
        assertEquals(full, xTicks(h))
    }

    @Test
    fun showTouchMarkerIgnoresXOutsideZoomWindow() {
        val (h, _) = holder(TestFixtures.paceAndHeartRate)
        h.zoomToRange(1.0..3.0)
        assertFalse(show(h, 4.5)) // 창 밖
        assertNull(h.activeMarkerRawX)
    }

    @Test
    fun showTouchMarkerToleratesFloatEdgeJustOutsideWindow() {
        val (h, _) = holder(TestFixtures.paceAndHeartRate)
        h.zoomToRange(1.0..3.0)
        // 상한 + 수 ulp: epsilon 이내는 드롭이 아니라 클램프.
        assertTrue(show(h, 3.0 + Math.ulp(3.0) * 4))
    }

    @Test
    fun zoomToRangeHidesExistingMarker() {
        val (h, _) = holder(TestFixtures.paceAndHeartRate)
        show(h, 2.0)
        assertEquals(2.0, h.activeMarkerRawX)
        h.zoomToRange(1.0..3.0)
        assertNull(h.activeMarkerRawX, "줌 진입 시 마커 숨김")
    }

    @Test
    fun pinchZoomInStillWorksAfterMomentarilyPassingThroughFullDomain() {
        val (h, _) = holder(TestFixtures.paceAndHeartRate)
        val full = xTicks(h)
        h.pinchBegan()
        h.pinchChanged(cumulativeScale = 0.9, anchor = 0.5) // 축소 → 전체 구간 클램프
        h.pinchChanged(cumulativeScale = 2.0, anchor = 0.5) // 같은 제스처로 다시 확대
        h.pinchEnded()
        assertNotEquals(full, xTicks(h), "핀치 확대가 살아 있어야 함")
    }

    @Test
    fun pinchEndAtFullDomainRestoresCleanState() {
        val (h, _) = holder(TestFixtures.paceAndHeartRate)
        val full = xTicks(h)
        h.pinchBegan()
        h.pinchChanged(cumulativeScale = 0.5, anchor = 0.5) // 축소 → 전체 구간
        h.pinchEnded()
        assertEquals(full, xTicks(h))
        h.pinchBegan()
        h.pinchChanged(cumulativeScale = 2.0, anchor = 0.5)
        h.pinchEnded()
        assertNotEquals(full, xTicks(h))
    }

    @Test
    fun zoomedPanShiftsWindowWithoutDrift() {
        val (h, _) = holder(TestFixtures.paceAndHeartRate)
        h.zoomToRange(1.0..3.0)
        val before = xTicks(h)
        h.panBegan()
        h.panChanged(fraction = -0.25) // 왼쪽 드래그 = 다음(오른쪽) 구간
        val shifted = xTicks(h)
        assertNotEquals(before, shifted)
        // 같은 제스처에서 0으로 되돌리면 원래 창으로 복귀(드리프트 없음).
        h.panChanged(fraction = 0.0)
        assertEquals(before, xTicks(h))
        h.panEnded()
    }

    @Test
    fun horizontalDominantPredicate() {
        assertTrue(isHorizontalDominant(10f, 3f))
        assertFalse(isHorizontalDominant(3f, 10f))
    }

    @Test
    fun resetZoomOnNonZoomedIsNoOp() {
        val (h, _) = holder(TestFixtures.paceAndHeartRate)
        val full = xTicks(h)
        h.resetZoom()
        assertEquals(full, xTicks(h))
        assertNull(h.zoom)
    }
}
