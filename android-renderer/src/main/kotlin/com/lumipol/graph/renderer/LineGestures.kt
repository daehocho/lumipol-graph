// iOS: RDChartView.swift 제스처부(5-인식기 + delegate 조율) — Compose 단일 pointerInput 재작성.
//
// **직역이 아니라 재작성이다.** iOS는 pan/markerTap/pinch/doubleTap/longPress 5개 인식기를
// `require(toFail:)`·`shouldRecognizeSimultaneouslyWith`·`gestureRecognizerShouldBegin`으로 조율한다.
// Compose엔 인식기 조율 프레임워크가 없으므로 단일 [awaitEachGesture] 수동 상태머신 + consume 제어로
// 근사한다. 콜백 계약·줌 수학·불변식은 [LineChartInteraction]에 캡슐화되어 iOS와 정확히 일치한다.
//
// iOS와 감각이 다를 수 있는 근사(kotlin-summary "제스처 근사 차이" 참조):
//  - 롱프레스 타임아웃·터치 슬롭·더블탭 타임아웃은 하드코딩이 아니라 `viewConfiguration` 시스템 값 사용.
//  - 단일탭이 더블탭 실패를 기다리는 지연(iOS require(toFail:))은 더블탭 타임아웃 창으로 근사.
package com.lumipol.graph.renderer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.LineChartLayout
import kotlin.math.abs

/** 확대 팬은 가로 우세 움직임만 시작(세로는 부모 스크롤로) — iOS `isHorizontalDominant`. */
internal fun isHorizontalDominant(dx: Float, dy: Float): Boolean = abs(dx) > abs(dy)

/**
 * 라인차트 제스처 루프. [PointerInputScope]에서 실행한다. layout/plot은 매 프레임 바뀔 수 있으므로
 * 최신값을 [layoutProvider]/[plotProvider]로 지연 조회한다(스크럽 좌표계를 그리기와 일치시킴).
 *
 * @param haptics 확대 상태 롱프레스 스크럽 진입 햅틱(iOS medium impact ≈ Android LongPress).
 */
internal suspend fun PointerInputScope.lineChartGestures(
    interaction: LineChartInteraction,
    data: LineChartData,
    style: ChartStyle,
    isZoomEnabled: Boolean,
    formatter: (ChartAxis, Double) -> String,
    haptics: HapticFeedback,
    layoutProvider: () -> LineChartLayout,
    plotProvider: () -> PlotArea?,
) {
    val slop = viewConfiguration.touchSlop
    val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
    val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis

    fun contextFor(): TouchMarkerContext? {
        val plot = plotProvider() ?: return null
        return TouchMarkerContext(
            data, layoutProvider(), style, plot, formatter,
            axisBySeriesId = interaction.axisBySeriesId,
            roleBySeriesId = interaction.roleBySeriesId,
        )
    }

    /** 손가락 픽셀 x → 원본 도메인 x. 플롯·X스케일 없으면 null. */
    fun rawXAt(px: Float): Double? {
        val plot = plotProvider() ?: return null
        val xTicks = layoutProvider().ticksFor(ChartAxis.X) ?: return null
        val xScale = AxisScale.from(xTicks) ?: return null
        return xScale.value(plot.normalizedX(px.toDouble()))
    }

    fun scrub(px: Float) {
        val rawX = rawXAt(px) ?: return
        val ctx = contextFor() ?: return
        interaction.scrubTo(rawX, ctx, notify = true)
    }

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val zoomedAtStart = interaction.zoom?.isZoomed == true

        // 1단계: 롱프레스 타임아웃 안에서 두 번째 포인터(핀치)·슬롭 초과 드래그·업(탭)을 감지.
        var secondPointer = false
        var slopDelta: Offset? = null
        var longPress = false
        try {
            withTimeout(longPressTimeoutMs) {
                while (true) {
                    val event = awaitPointerEvent()
                    if (isZoomEnabled && event.changes.count { it.pressed } >= 2) {
                        secondPointer = true
                        return@withTimeout
                    }
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) return@withTimeout // up → 탭
                    val delta = change.position - down.position
                    if (delta.getDistance() > slop) {
                        slopDelta = delta
                        return@withTimeout
                    }
                }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            longPress = true
        }

        val delta = slopDelta
        when {
            secondPointer -> pinchLoop(interaction, isZoomEnabled) { px ->
                plotProvider()?.normalizedX(px.toDouble()) ?: 0.5
            }

            longPress -> {
                // 확대 상태에서만 "값 조회 진입" 햅틱(비확대 드래그 스크럽은 무햅틱 — iOS 동일).
                if (zoomedAtStart) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                val escalated = scrubLoop(
                    interaction, down.id, ::scrub,
                    downPx = down.position.x, escalateToPinch = isZoomEnabled,
                )
                if (escalated) {
                    pinchLoop(interaction, isZoomEnabled) { px ->
                        plotProvider()?.normalizedX(px.toDouble()) ?: 0.5
                    }
                }
            }

            delta != null -> {
                // 확대/비확대 공통: 가로 우세 드래그만 차트가 잡고(스크럽/팬 + consume), 세로 우세는
                // 미진입·미소비 → 부모 세로 스크롤에 양보(Arch M1 / UX Major-2). iOS 비확대 pan은
                // `shouldRecognizeSimultaneouslyWith=true`로 세로 스크롤과 동시 인식되는데, 세로를 트랩하지
                // 않고 부모에 넘기는 것이 그 동시성에 더 가깝다(스크롤 컨테이너 임베드 시 회귀 방지).
                if (isHorizontalDominant(delta.x, delta.y)) {
                    if (zoomedAtStart) {
                        panLoop(interaction, down.id) {
                            plotProvider()?.width?.toFloat() ?: 0f
                        }
                    } else {
                        val escalated = scrubLoop(
                            interaction, down.id, ::scrub,
                            downPx = down.position.x, escalateToPinch = isZoomEnabled,
                        )
                        if (escalated) {
                            pinchLoop(interaction, isZoomEnabled) { px ->
                                plotProvider()?.normalizedX(px.toDouble()) ?: 0.5
                            }
                        }
                    }
                }
            }

            else -> {
                // 탭. 확대 가능하면 더블탭(줌 리셋)을 기다렸다가, 아니면 단일탭 스크럽(마커 표시).
                if (isZoomEnabled && awaitSecondTap(doubleTapTimeoutMs)) {
                    interaction.resetZoom()
                } else {
                    scrub(down.position.x)
                }
            }
        }
    }
}

/**
 * 스크럽 루프 — 진입 즉시 1회 스크럽 후 이동마다 갱신, 모든 포인터가 떨어지면 종료 통지.
 *
 * @param escalateToPinch 스크럽 도중 두 번째 포인터가 닿으면 스크럽을 끝내고 true 반환 —
 *   호출부가 pinchLoop로 이어간다(롱프레스 타임아웃 뒤 늦게 시작하는 핀치도 줌이어야 함, iOS
 *   독립 핀치 인식기 parity).
 * @return 핀치 승격이 필요하면 true.
 */
private suspend fun AwaitPointerEventScope.scrubLoop(
    interaction: LineChartInteraction,
    downId: PointerId,
    scrub: (Float) -> Unit,
    downPx: Float,
    escalateToPinch: Boolean = false,
): Boolean {
    // endScrub는 finally에서 — 스크럽 도중 데이터 갱신으로 pointerInput이 재시작(코루틴 취소)돼도
    // onScrub/onScrubEnd 짝 불변식이 지켜져야 한다(dangling onScrub 방지). endScrub는 마커가
    // 표시 중일 때만 발화하므로 정상 종료·핀치 승격 경로에서 중복 통지는 없다.
    try {
        scrub(downPx)
        while (true) {
            val event = awaitPointerEvent()
            if (escalateToPinch && event.changes.count { it.pressed } >= 2) return true
            val change = event.changes.firstOrNull { it.id == downId }
            // 스크럽에 진입한 시점(가로 우세 드래그·롱프레스)은 이미 차트 의도이므로, 이후 이동은 consume해
            // 부모 스크롤로 새지 않게 한다. 세로 우세 드래그는 애초에 이 루프에 진입하지 않아 부모가 스크롤한다.
            change?.let { if (it.positionChanged()) it.consume() }
            if (change != null && change.pressed) scrub(change.position.x)
            if (event.changes.none { it.pressed }) break
        }
    } finally {
        interaction.endScrub()
    }
    return false
}

/**
 * 확대 팬 루프 — 시작점 대비 누적 이동 비율. 가로 이동을 consume(차트 독점).
 *
 * @param plotWidth 비율의 분모인 **플롯 폭**(insets 제외) 최신값 — 전체 캔버스 폭으로 나누면
 *   iOS(`translation.x / plotArea.rect.width`, RDChartView.swift) 대비 팬 게인이 인셋 비율만큼 작아진다.
 */
private suspend fun AwaitPointerEventScope.panLoop(
    interaction: LineChartInteraction,
    downId: PointerId,
    plotWidth: () -> Float,
) {
    interaction.panBegan()
    var startX: Float? = null
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == downId }
        if (change != null) {
            // 시작점 = 첫 프레임의 이전 위치(제스처 시작 시점).
            if (startX == null) startX = change.previousPosition.x
            if (change.positionChanged()) change.consume()
            val width = plotWidth()
            if (change.pressed && width > 0f) {
                // 오른쪽 드래그(+) = 창 왼쪽 이동. 시작점 대비 누적 이동/플롯폭.
                val fraction = (change.position.x - startX!!) / width
                interaction.panChanged(fraction.toDouble())
            }
        }
        if (event.changes.none { it.pressed }) break
    }
    interaction.panEnded()
}

/**
 * 핀치 루프 — 두 포인터 거리비를 누적 배율로, 중점 x를 앵커로 매 프레임 재계산(하이브리드 줌).
 * 콘텐츠 transform이 아니라 [LineChartInteraction.pinchChanged]로 창을 옮겨 코어를 재계산한다.
 */
private suspend fun AwaitPointerEventScope.pinchLoop(
    interaction: LineChartInteraction,
    isZoomEnabled: Boolean,
    anchorOf: (Float) -> Double,
) {
    if (!isZoomEnabled) return
    interaction.pinchBegan()
    var cumulativeScale = 1.0
    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.count { it.pressed }
        if (pressed >= 2) {
            val zoom = event.calculateZoom()
            if (zoom > 0f && zoom.isFinite()) cumulativeScale *= zoom.toDouble()
            val centroid = event.calculateCentroid(useCurrent = true)
            val anchor = if (centroid != Offset.Unspecified) anchorOf(centroid.x) else 0.5
            interaction.pinchChanged(cumulativeScale, anchor)
            event.changes.forEach { if (it.positionChanged()) it.consume() }
        }
        if (event.changes.none { it.pressed } || pressed < 2) break
    }
    interaction.pinchEnded()
}

/** 업 이후 [timeoutMillis] 안에 두 번째 down(탭)이 오면 true — 더블탭 근사. */
private suspend fun AwaitPointerEventScope.awaitSecondTap(timeoutMillis: Long): Boolean {
    return try {
        withTimeout(timeoutMillis) {
            awaitFirstDown(requireUnconsumed = false)
            true
        }
    } catch (_: PointerEventTimeoutCancellationException) {
        false
    }
}
