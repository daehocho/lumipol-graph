package com.lumipol.graph.interaction

import kotlin.math.max
import kotlin.math.min

/**
 * X축 줌 창 산술 — 핀치·팬 제스처 값을 도메인 구간으로 환산하고 전체 범위·최대 배율로
 * 클램프한다(B3 — 양 렌더러 ZoomState 중복의 코어 이관, 숫자·클램프 규칙 불변).
 *
 * 순수 산술만 담당한다: 제스처 **인식**(슬롭·타임아웃·인식기 조율)과 상태 보유(현재 창 저장,
 * 기준 창 스냅샷)는 플랫폼 소관. 불변 값 타입이라 모든 변형은 새 인스턴스를 반환한다.
 *
 * 라이브 제스처는 기준 창(제스처 시작 스냅샷) + 누적값으로 매 프레임 재계산해 드리프트를
 * 막는다 — [pinch]/[pan]이 기준 창을 인자로 받는 이유.
 */
data class ZoomWindow(
    val fullMin: Double,
    val fullMax: Double,
    val windowMin: Double,
    val windowMax: Double,
) {
    /** 전체 도메인으로 초기화(창 = 전체). */
    constructor(fullMin: Double, fullMax: Double) : this(fullMin, fullMax, fullMin, fullMax)

    /** 현재 창이 전체 범위와 다르면 확대 상태. */
    val isZoomed: Boolean get() = windowMin != fullMin || windowMax != fullMax

    /** 현재 배율(1 = 전체 보기). */
    val scale: Double get() = fullSpan / span

    private val span: Double get() = windowMax - windowMin
    private val fullSpan: Double get() = fullMax - fullMin

    /**
     * 기준 창([startMin]..[startMax], 제스처 시작 스냅샷)에서 누적 배율·앵커로 새 창 계산.
     * [anchor]는 플롯 내 위치(0~1) — 그 지점의 도메인 값이 제자리에 남는다.
     * [cumulativeScale]이 0 이하면 무시(현 상태 그대로 반환). [maxScale]은 전체 대비 최대 배율.
     */
    fun pinch(
        startMin: Double,
        startMax: Double,
        cumulativeScale: Double,
        anchor: Double,
        maxScale: Double,
    ): ZoomWindow {
        if (cumulativeScale <= 0) return this
        val startSpan = startMax - startMin
        val targetSpan = min(max(startSpan / cumulativeScale, fullSpan / maxScale), fullSpan)
        val anchorValue = startMin + anchor * startSpan
        return place(lower = anchorValue - anchor * targetSpan, span = targetSpan)
    }

    /**
     * 기준 창에서 플롯 폭 대비 누적 이동 비율([fraction], 오른쪽 드래그 = +)만큼 왼쪽 이동 —
     * "오른쪽 드래그 = 이전 구간". 폭은 기준 창 그대로.
     */
    fun pan(startMin: Double, startMax: Double, fraction: Double): ZoomWindow {
        val startSpan = startMax - startMin
        return place(lower = startMin - fraction * startSpan, span = startSpan)
    }

    /**
     * 프로그래매틱 줌 — 폭 유지한 채 전체 범위로 클램프.
     * 역전·퇴화 구간([targetMax] ≤ [targetMin])은 무시(현 상태 그대로 반환, [pinch]의 무효
     * 배율 규칙과 동일) — 음수 span이 place에 들어가면 windowMax < windowMin 창이 만들어져
     * isZoomed는 true인데 레이아웃은 전체로 폴백하는 모순 상태가 된다.
     */
    fun setWindow(targetMin: Double, targetMax: Double): ZoomWindow {
        if (targetMax <= targetMin) return this
        return place(lower = targetMin, span = min(targetMax - targetMin, fullSpan))
    }

    /** 전체 범위로 되돌린다. */
    fun reset(): ZoomWindow = copy(windowMin = fullMin, windowMax = fullMax)

    private fun place(lower: Double, span: Double): ZoomWindow {
        // 전체 폭이면 fullDomain을 그대로 사용 — lower+(upper-lower) 재구성은 1 ulp 어긋날 수
        // 있어 isZoomed(정확한 동등성)가 영영 true로 남고 스크럽이 팬으로 오라우팅된다.
        if (span >= fullSpan) return copy(windowMin = fullMin, windowMax = fullMax)
        val clamped = min(max(lower, fullMin), fullMax - span)
        return copy(windowMin = clamped, windowMax = clamped + span)
    }
}
