package com.lumipol.graph.interaction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 스크럽 tick 게이트 — 픽셀 격자(1차 기준) + 시간 상한(플릭 포화 방지) 판정 규칙 고정.
 * 기대값은 설계 문서(2026-07-31-line-chart-scrub-haptics-design) 상태표와 1:1 대응한다.
 */
class ScrubHapticGateTest {
    private val spacing = 12.0
    private val interval = 35L

    /** 스냅 지점을 픽셀과 함께 움직이는 기본 헬퍼 — 스냅 정지 케이스는 snappedX를 명시한다. */
    private fun ScrubHapticGate.stepAt(px: Double, nowMs: Long, snappedX: Double = px) =
        step(px, snappedX, nowMs, spacing, interval)

    @Test
    fun firstFrameAnchorsWithoutFiring() {
        val step = ScrubHapticGate().stepAt(100.0, 1000L)
        assertFalse(step.fire, "스크럽 첫 프레임(드래그 시작)은 무발화")
        assertEquals(100.0, step.gate.anchorPx)
        assertNull(step.gate.lastFireMs)
    }

    @Test
    fun belowSpacingDoesNotFireAndKeepsGate() {
        val anchored = ScrubHapticGate().stepAt(100.0, 1000L).gate
        val step = anchored.stepAt(111.9, 1100L)
        assertFalse(step.fire)
        assertEquals(anchored, step.gate, "격자 미달이면 게이트 무변화")
    }

    @Test
    fun spacingReachedFiresAndReanchors() {
        val anchored = ScrubHapticGate().stepAt(100.0, 1000L).gate
        val step = anchored.stepAt(112.0, 1001L)
        assertTrue(step.fire, "정확히 12pt도 발화(>= 경계)")
        assertEquals(112.0, step.gate.anchorPx)
        assertEquals(1001L, step.gate.lastFireMs)
    }

    @Test
    fun reverseDirectionUsesAbsoluteDistance() {
        val anchored = ScrubHapticGate().stepAt(100.0, 1000L).gate
        val step = anchored.stepAt(88.0, 1001L)
        assertTrue(step.fire, "왼쪽 이동도 동일 격자")
        assertEquals(88.0, step.gate.anchorPx)
    }

    @Test
    fun throttleSuppressesFireButKeepsAnchorSoNextTickIsImmediate() {
        val fired = ScrubHapticGate().stepAt(100.0, 1000L).gate.stepAt(112.0, 1000L)
        assertTrue(fired.fire, "제스처 첫 tick은 lastFireMs가 없어 시간 조건 면제")

        val throttled = fired.gate.stepAt(124.0, 1020L) // 12pt 이동, 20ms 경과
        assertFalse(throttled.fire, "35ms 미달이면 발화 없음")
        assertEquals(112.0, throttled.gate.anchorPx, "앵커 유지 — 다음 tick이 즉시 나와야 함")
        assertEquals(1000L, throttled.gate.lastFireMs)

        val next = throttled.gate.stepAt(124.0, 1035L) // 추가 이동 없이 35ms 도달
        assertTrue(next.fire, "시간이 차면 추가 이동 없이 발화")
        assertEquals(124.0, next.gate.anchorPx)
        assertEquals(1035L, next.gate.lastFireMs)
    }

    @Test
    fun frozenSnapDoesNotFireHoweverFarTheFingerMoves() {
        // 그래프 끝(마지막 샘플) 이후 계속 옆으로 미는 상황 — 플롯 밖은 nx가 0~1로 클램프돼
        // 마커는 마지막 샘플에 얼어붙는다. 조회 지점이 안 움직이면 진동도 없어야 한다.
        val fired = ScrubHapticGate().stepAt(100.0, 1000L).gate.stepAt(112.0, 1000L, snappedX = 112.0)
        assertTrue(fired.fire)

        var gate = fired.gate
        for (i in 1..20) {
            // 손가락은 계속 오른쪽(+40pt/프레임), 스냅은 112.0에 고정.
            val step = gate.stepAt(112.0 + i * 40.0, 1000L + i * 50L, snappedX = 112.0)
            assertFalse(step.fire, "스냅 정지 중에는 발화 없음(프레임 $i)")
            gate = step.gate
        }
        // 스냅이 다시 움직이면 즉시 발화(앵커·시각이 정지 구간에 오염되지 않았다).
        val resumed = gate.stepAt(920.0, 3000L, snappedX = 200.0)
        assertTrue(resumed.fire)
        assertEquals(200.0, resumed.gate.lastSnappedX)
    }

    @Test
    fun firstFrameRecordsSnapSoStayingOnSameSampleIsSilent() {
        val anchored = ScrubHapticGate().stepAt(100.0, 1000L, snappedX = 50.0).gate
        assertEquals(50.0, anchored.lastSnappedX, "첫 프레임은 스냅 지점을 기록한다")
        val step = anchored.stepAt(140.0, 1100L, snappedX = 50.0)
        assertFalse(step.fire, "같은 샘플 위를 지나는 동안은 무발화")
    }

    @Test
    fun fastFlickIsRateLimitedNotCoalescedIntoBurst() {
        // 프레임당 40pt(≈500pt/s)로 8프레임 훑기 — 8ms 간격이면 35ms마다 1회로 제한된다.
        var gate = ScrubHapticGate().stepAt(0.0, 0L).gate
        var fires = 0
        for (i in 1..8) {
            val step = gate.stepAt(i * 40.0, i * 8L)
            gate = step.gate
            if (step.fire) fires++
        }
        assertEquals(2, fires, "56ms 동안 첫 tick + 35ms 경과 후 1회 = 2회")
    }
}
