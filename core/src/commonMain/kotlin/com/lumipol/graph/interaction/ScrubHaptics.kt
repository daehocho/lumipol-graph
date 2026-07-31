package com.lumipol.graph.interaction

import kotlin.math.abs

/**
 * 라인차트 스크럽 tick 햅틱 게이트 — "지금 진동을 울릴 순간인가"만 판정하는 순수 산술
 * ([ZoomWindow]와 동일 doctrine: 제스처 인식·진동 발생·시각 취득은 플랫폼 소관).
 *
 * 왜 픽셀 격자인가: 라인차트의 스냅 격자는 샘플 포인트(1초 샘플 40분 러닝이면 2400개)라
 * "스냅이 바뀔 때마다"로 울리면 초당 수십 번 발화해 진동이 연속음으로 뭉개진다. 샘플 밀도는
 * GPS 상태에 따라 들쭉날쭉해 감각도 불균일하다. 고정 픽셀 간격은 데이터 밀도·줌 배율·러닝
 * 길이와 무관하게 항상 같은 손가락 감각을 준다.
 *
 * 왜 시간 상한도 필요한가: 빠른 플릭은 초당 500pt 이상 움직여 12pt 격자로도 40Hz를 넘는다.
 * 스로틀에 걸린 프레임은 **앵커를 갱신하지 않으므로** 시간이 차는 즉시 발화한다 — 격자가 1차
 * 기준이고 시간은 상한으로만 작동하며, 밀린 tick이 몰려 나오는 catch-up은 없다.
 *
 * 불변 값 타입이라 모든 전이는 새 인스턴스를 반환한다.
 */
data class ScrubHapticGate(
    /** 마지막 발화 지점(플랫폼 픽셀). null이면 스크럽 첫 프레임 — 앵커만 잡고 발화하지 않는다. */
    val anchorPx: Double? = null,
    /** 마지막 발화 시각(ms). null이면 이 제스처에서 아직 발화하지 않았다(첫 tick은 시간 조건 면제). */
    val lastFireMs: Long? = null,
    /** 마지막으로 진동을 낸 조회 지점(스냅된 도메인 x). 같은 지점에 머무는 동안은 무발화. */
    val lastSnappedX: Double? = null,
) {
    /** 전이 결과 — 갱신된 게이트와 이번 프레임 발화 여부. */
    data class Step(val gate: ScrubHapticGate, val fire: Boolean)

    /**
     * 스크럽 프레임 판정.
     *
     * @param px 손가락의 플랫폼 픽셀 x(iOS pt / AOS density 배율 캔버스 px).
     * @param snappedX 이 프레임의 조회 지점(마커가 실제로 선 스냅 도메인 x). 손가락이 움직여도
     *   이 값이 그대로면 발화하지 않는다 — 그래프 끝을 넘겨 계속 밀 때(플롯 밖 nx는 0~1로
     *   클램프되어 마지막 샘플에 얼어붙는다)와 성긴 샘플 구간의 헛진동을 막는 조건이다.
     * @param nowMs 플랫폼이 주입하는 단조 시각 — 코어에 시계를 두지 않아 테스트가 결정론적이다.
     * @param spacingPx tick 격자([com.lumipol.graph.ChartDefaults.SCRUB_HAPTIC_SPACING_DP] 환산값).
     * @param minIntervalMs 발화 최소 간격([com.lumipol.graph.ChartDefaults.SCRUB_HAPTIC_MIN_INTERVAL_MS]).
     */
    fun step(px: Double, snappedX: Double, nowMs: Long, spacingPx: Double, minIntervalMs: Long): Step {
        val anchor = anchorPx
            ?: return Step(ScrubHapticGate(px, lastFireMs, snappedX), false)
        if (snappedX == lastSnappedX) return Step(this, false)
        if (abs(px - anchor) < spacingPx) return Step(this, false)
        val last = lastFireMs
        if (last != null && nowMs - last < minIntervalMs) return Step(this, false)
        return Step(ScrubHapticGate(px, nowMs, snappedX), true)
    }
}
