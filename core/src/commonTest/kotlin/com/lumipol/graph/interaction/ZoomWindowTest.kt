package com.lumipol.graph.interaction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * B3 — 렌더러 ZoomState(iOS ZoomStateTests.swift / AOS ZoomStateTest.kt)에서 이식.
 * 기대값 숫자를 기존 양 플랫폼 테스트와 동일하게 유지한다.
 * 단발 pinch/pan 미러 API는 코어 이관에서 소거 — 기준 창 = 현재 창으로 동일 산술을 표현한다.
 */
class ZoomWindowTest {
    private fun makeState() = ZoomWindow(0.0, 10.0)

    private val eps = 1e-9

    /** 현재 창 기준 단발 핀치(구 pinch(gestureScale:)와 동일 산술). */
    private fun ZoomWindow.pinchBy(scale: Double, anchor: Double, maxScale: Double = 10.0) =
        pinch(windowMin, windowMax, scale, anchor, maxScale)

    private fun ZoomWindow.panBy(fraction: Double) = pan(windowMin, windowMax, fraction)

    @Test
    fun initialStateIsFullDomain() {
        val state = makeState()
        assertEquals(0.0, state.windowMin)
        assertEquals(10.0, state.windowMax)
        assertFalse(state.isZoomed)
        assertEquals(1.0, state.scale, eps)
    }

    @Test
    fun pinchInAtCenterHalvesWindowAroundCenter() {
        val state = makeState().pinchBy(2.0, anchor = 0.5)
        assertEquals(2.5, state.windowMin, eps)
        assertEquals(7.5, state.windowMax, eps)
        assertEquals(2.0, state.scale, eps)
    }

    @Test
    fun pinchAnchorValueStaysPut() {
        // anchor 0.25 → 도메인 값 2.5가 확대 후에도 창의 25% 지점에 남는다
        val state = makeState().pinchBy(2.0, anchor = 0.25)
        val anchorValue = state.windowMin + 0.25 * (state.windowMax - state.windowMin)
        assertEquals(2.5, anchorValue, eps)
    }

    @Test
    fun pinchClampsAtMaxScale() {
        val state = makeState().pinchBy(100.0, anchor = 0.5)
        assertEquals(10.0, state.scale, eps)
    }

    @Test
    fun pinchOutClampsToFullDomain() {
        val state = makeState().pinchBy(2.0, anchor = 0.5).pinchBy(0.1, anchor = 0.5)
        assertEquals(0.0, state.windowMin)
        assertEquals(10.0, state.windowMax)
        assertFalse(state.isZoomed)
    }

    @Test
    fun panMovesWindowOppositeToDragAndClamps() {
        var state = makeState().pinchBy(2.0, anchor = 0.5) // 2.5..7.5
        state = state.panBy(0.2) // 오른쪽 드래그 → 이전(왼쪽) 구간으로
        assertEquals(1.5, state.windowMin, eps)
        state = state.panBy(10.0) // 크게 드래그 → 왼쪽 끝 클램프
        assertEquals(0.0, state.windowMin, eps)
        state = state.panBy(-10.0) // 반대로 → 오른쪽 끝 클램프
        assertEquals(10.0, state.windowMax, eps)
    }

    @Test
    fun setWindowClampsToFullDomain() {
        val state = makeState().setWindow(8.0, 13.0)
        assertEquals(5.0, state.windowMin, eps) // 폭 5 유지, 오른쪽 끝 클램프
        assertEquals(10.0, state.windowMax, eps)
    }

    @Test
    fun setWindowWiderThanFullDomainClampsToFull() {
        val state = makeState().setWindow(-5.0, 20.0)
        assertEquals(0.0, state.windowMin)
        assertEquals(10.0, state.windowMax)
    }

    @Test
    fun setWindowIgnoresInvertedOrDegenerateRange() {
        // 역전(max < min)·퇴화(max == min) 구간은 무시 — pinch의 무효 배율 규칙과 동일.
        val zoomed = makeState().setWindow(3.0, 5.0)
        assertEquals(zoomed, zoomed.setWindow(5.0, 3.0))
        assertEquals(zoomed, zoomed.setWindow(4.0, 4.0))
    }

    @Test
    fun resetRestoresFullDomain() {
        val state = makeState().pinchBy(3.0, anchor = 0.3).reset()
        assertEquals(0.0, state.windowMin)
        assertEquals(10.0, state.windowMax)
    }

    @Test
    fun zeroOrNegativePinchScaleIsIgnored() {
        val state = makeState().pinchBy(0.0, anchor = 0.5)
        assertEquals(0.0, state.windowMin)
        assertEquals(10.0, state.windowMax)
    }

    // 라이브 핀치 (기준 창 + 누적 배율)

    @Test
    fun pinchFromStartIsCumulativeAndAnchored() {
        val start = makeState() // 0..10
        var state = makeState().pinch(start.windowMin, start.windowMax, 2.0, anchor = 0.5, maxScale = 10.0)
        assertEquals(2.5, state.windowMin, eps)
        assertEquals(7.5, state.windowMax, eps)
        // 같은 기준 창에서 배율만 키우면 누적(드리프트 없음)
        state = state.pinch(start.windowMin, start.windowMax, 4.0, anchor = 0.5, maxScale = 10.0)
        assertEquals(3.75, state.windowMin, eps)
        assertEquals(6.25, state.windowMax, eps)
    }

    @Test
    fun pinchFromFullZoomOutStaysFull() {
        val state = makeState().pinchBy(0.5, anchor = 0.5)
        assertEquals(0.0, state.windowMin)
        assertEquals(10.0, state.windowMax)
        assertFalse(state.isZoomed)
    }

    @Test
    fun fullZoomOutRestoresExactFullDomainDespiteFloatRounding() {
        // start + (end - start)가 end와 1 ulp 어긋나는 실수 도메인 —
        // 완전 줌아웃 후에도 창이 fullDomain과 정확히 같아야 스크럽이 복구된다.
        var state = ZoomWindow(21.730886, 195.28034191195613)
        state = state.pinchBy(4.0, anchor = 0.7)
        assertTrue(state.isZoomed)
        state = state.pinchBy(0.1, anchor = 0.3)
        assertEquals(21.730886, state.windowMin)
        assertEquals(195.28034191195613, state.windowMax)
        assertFalse(state.isZoomed)
    }

    @Test
    fun pinchFromRespectsMaxScale() {
        val state = makeState().pinchBy(100.0, anchor = 0.5)
        assertEquals(10.0, state.scale, eps)
    }
}
