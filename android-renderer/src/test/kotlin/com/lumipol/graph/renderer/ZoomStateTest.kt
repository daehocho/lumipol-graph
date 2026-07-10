package com.lumipol.graph.renderer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// iOS: ZoomStateTests.swift — 기대값 숫자를 iOS 테스트와 동일하게 유지.
class ZoomStateTest {
    private fun makeState(): ZoomState = ZoomState.full(0.0..10.0)

    private val eps = 1e-9

    @Test
    fun initialStateIsFullDomain() {
        val state = makeState()
        assertEquals(0.0..10.0, state.window)
        assertFalse(state.isZoomed)
        assertEquals(1.0, state.scale, eps)
    }

    @Test
    fun pinchInAtCenterHalvesWindowAroundCenter() {
        val state = makeState().pinch(gestureScale = 2.0, anchor = 0.5, maxScale = 10.0)
        assertEquals(2.5, state.window.start, eps)
        assertEquals(7.5, state.window.endInclusive, eps)
        assertEquals(2.0, state.scale, eps)
    }

    @Test
    fun pinchAnchorValueStaysPut() {
        // anchor 0.25 → 도메인 값 2.5가 확대 후에도 창의 25% 지점에 남는다
        val state = makeState().pinch(gestureScale = 2.0, anchor = 0.25, maxScale = 10.0)
        val anchorValue = state.window.start + 0.25 * (state.window.endInclusive - state.window.start)
        assertEquals(2.5, anchorValue, eps)
    }

    @Test
    fun pinchClampsAtMaxScale() {
        val state = makeState().pinch(gestureScale = 100.0, anchor = 0.5, maxScale = 10.0)
        assertEquals(10.0, state.scale, eps)
    }

    @Test
    fun pinchOutClampsToFullDomain() {
        val state = makeState()
            .pinch(gestureScale = 2.0, anchor = 0.5, maxScale = 10.0)
            .pinch(gestureScale = 0.1, anchor = 0.5, maxScale = 10.0)
        assertEquals(0.0..10.0, state.window)
        assertFalse(state.isZoomed)
    }

    @Test
    fun panMovesWindowOppositeToDragAndClamps() {
        var state = makeState().pinch(gestureScale = 2.0, anchor = 0.5, maxScale = 10.0) // 2.5..7.5
        state = state.pan(fraction = 0.2) // 오른쪽 드래그 → 이전(왼쪽) 구간으로
        assertEquals(1.5, state.window.start, eps)
        state = state.pan(fraction = 10.0) // 크게 드래그 → 왼쪽 끝 클램프
        assertEquals(0.0, state.window.start, eps)
        state = state.pan(fraction = -10.0) // 반대로 → 오른쪽 끝 클램프
        assertEquals(10.0, state.window.endInclusive, eps)
    }

    @Test
    fun setWindowClampsToFullDomain() {
        val state = makeState().setWindow(8.0..13.0)
        assertEquals(5.0..10.0, state.window) // 폭 5 유지, 오른쪽 끝 클램프
    }

    @Test
    fun setWindowWiderThanFullDomainClampsToFull() {
        val state = makeState().setWindow(-5.0..20.0)
        assertEquals(0.0..10.0, state.window)
    }

    @Test
    fun resetRestoresFullDomain() {
        val state = makeState()
            .pinch(gestureScale = 3.0, anchor = 0.3, maxScale = 10.0)
            .reset()
        assertEquals(0.0..10.0, state.window)
    }

    @Test
    fun zeroOrNegativePinchScaleIsIgnored() {
        val state = makeState().pinch(gestureScale = 0.0, anchor = 0.5, maxScale = 10.0)
        assertEquals(0.0..10.0, state.window)
    }

    // MARK: - Live pinch (기준 창 + 누적 배율)

    @Test
    fun pinchFromStartIsCumulativeAndAnchored() {
        val start = makeState().window // 0..10
        var state = makeState()
            .pinch(startWindow = start, cumulativeScale = 2.0, anchor = 0.5, maxScale = 10.0)
        assertEquals(2.5, state.window.start, eps)
        assertEquals(7.5, state.window.endInclusive, eps)
        // 같은 기준 창에서 배율만 키우면 누적(드리프트 없음)
        state = state.pinch(startWindow = start, cumulativeScale = 4.0, anchor = 0.5, maxScale = 10.0)
        assertEquals(3.75, state.window.start, eps)
        assertEquals(6.25, state.window.endInclusive, eps)
    }

    @Test
    fun pinchFromFullZoomOutStaysFull() {
        val state = makeState().let {
            it.pinch(startWindow = it.window, cumulativeScale = 0.5, anchor = 0.5, maxScale = 10.0)
        }
        assertEquals(0.0..10.0, state.window)
        assertFalse(state.isZoomed)
    }

    @Test
    fun fullZoomOutRestoresExactFullDomainDespiteFloatRounding() {
        // start + (end - start)가 end와 1 ulp 어긋나는 실수 도메인 —
        // 완전 줌아웃 후에도 window가 fullDomain과 정확히 같아야 스크럽이 복구된다.
        val fullDomain = 21.730886..195.28034191195613
        var state = ZoomState.full(fullDomain)
        state = state.pinch(startWindow = state.window, cumulativeScale = 4.0, anchor = 0.7, maxScale = 10.0)
        kotlin.test.assertTrue(state.isZoomed)
        state = state.pinch(startWindow = state.window, cumulativeScale = 0.1, anchor = 0.3, maxScale = 10.0)
        assertEquals(fullDomain, state.window)
        assertFalse(state.isZoomed)
    }

    @Test
    fun pinchFromRespectsMaxScale() {
        val state = makeState().let {
            it.pinch(startWindow = it.window, cumulativeScale = 100.0, anchor = 0.5, maxScale = 10.0)
        }
        assertEquals(10.0, state.scale, eps)
    }
}
