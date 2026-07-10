// iOS: ZoomState.swift
package com.lumipol.graph.renderer

import kotlin.math.max
import kotlin.math.min

/**
 * X축 줌 상태 — 보이는 도메인 구간 계산 전담 (Compose 비의존, 순수 JVM 단위 테스트 대상).
 *
 * 핀치·팬 제스처 값을 도메인 구간으로 환산하고 전체 범위·최대 배율로 클램프한다.
 * iOS는 `mutating struct`였으나 Kotlin에서는 불변 [data class]로 표현하고 모든 변형은
 * 새 인스턴스를 반환한다(`copy`). 숫자·클램프 규칙은 iOS와 정확히 일치한다.
 */
internal data class ZoomState(
    val fullDomain: ClosedFloatingPointRange<Double>,
    val window: ClosedFloatingPointRange<Double>,
) {
    /** 현재 창이 전체 범위와 다르면 확대 상태. */
    val isZoomed: Boolean get() = window != fullDomain

    /** 현재 배율 (1 = 전체 보기). */
    val scale: Double get() = fullSpan / span

    private val span: Double get() = window.endInclusive - window.start
    private val fullSpan: Double get() = fullDomain.endInclusive - fullDomain.start

    /**
     * 핀치 확대/축소. [anchor]는 플롯 내 위치(0~1) — 그 지점의 도메인 값이 제자리에 남는다.
     * [gestureScale]이 0 이하면 무시(현 상태 그대로 반환).
     *
     * 프로덕션 제스처는 누적 배율 오버로드([pinch]의 startWindow판)만 쓰지만, iOS `ZoomState.pinch(by:)`
     * API 미러 + iOS 테스트 스위트와 숫자를 맞추는 스펙 고정용으로 유지한다([pan]/[scale]도 동일).
     */
    fun pinch(gestureScale: Double, anchor: Double, maxScale: Double): ZoomState {
        if (gestureScale <= 0) return this
        val targetSpan = min(max(span / gestureScale, fullSpan / maxScale), fullSpan)
        val anchorValue = window.start + anchor * span
        return place(lower = anchorValue - anchor * targetSpan, span = targetSpan)
    }

    /**
     * 기준 창([startWindow], 제스처 시작)에서 누적 배율·앵커로 새 창 계산 (라이브 핀치 프레임마다 호출).
     * [cumulativeScale]은 제스처 시작 대비 누적값이라 매 프레임 기준 창에서 다시 계산해 드리프트를 막는다.
     */
    fun pinch(
        startWindow: ClosedFloatingPointRange<Double>,
        cumulativeScale: Double,
        anchor: Double,
        maxScale: Double,
    ): ZoomState {
        if (cumulativeScale <= 0) return this
        val startSpan = startWindow.endInclusive - startWindow.start
        val targetSpan = min(max(startSpan / cumulativeScale, fullSpan / maxScale), fullSpan)
        val anchorValue = startWindow.start + anchor * startSpan
        return place(lower = anchorValue - anchor * targetSpan, span = targetSpan)
    }

    /** 플롯 폭 대비 드래그 비율만큼 좌우 이동 (오른쪽 드래그 = 이전 구간). */
    fun pan(fraction: Double): ZoomState =
        place(lower = window.start - fraction * span, span = span)

    /** 프로그래매틱 줌·테스트용 — 폭 유지한 채 전체 범위로 클램프. */
    fun setWindow(target: ClosedFloatingPointRange<Double>): ZoomState {
        val clampedSpan = min(target.endInclusive - target.start, fullSpan)
        return place(lower = target.start, span = clampedSpan)
    }

    /** 전체 범위로 되돌린다. */
    fun reset(): ZoomState = copy(window = fullDomain)

    private fun place(lower: Double, span: Double): ZoomState {
        // 전체 폭이면 fullDomain을 그대로 사용 — start+(end-start) 재구성은 1 ulp 어긋날 수
        // 있어 isZoomed(정확한 동등성)가 영영 true로 남고 스크럽이 팬으로 오라우팅된다.
        if (span >= fullSpan) return copy(window = fullDomain)
        val clamped = min(max(lower, fullDomain.start), fullDomain.endInclusive - span)
        return copy(window = clamped..(clamped + span))
    }

    companion object {
        /** 전체 도메인으로 초기화 (iOS `init(fullDomain:)`). */
        fun full(fullDomain: ClosedFloatingPointRange<Double>): ZoomState =
            ZoomState(fullDomain = fullDomain, window = fullDomain)
    }
}
