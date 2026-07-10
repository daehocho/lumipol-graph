// iOS: RDHeartRateZoneView.swift
//
// 심박존 분포 도넛. DonutEngine 레이아웃을 arc 스트로크로 렌더. 축/줌 없음. 탭→세그먼트 선택.
// 순수 조립·히트테스트([buildDonutArcs]/[donutSegmentIndex])는 JVM 단위테스트로 검증하고,
// composable [RDHeartRateZoneChart]는 그리기 + pointerInput 배선만 담당한다.
package com.lumipol.graph.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.lumipol.graph.DonutEngine
import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutChartLayout
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** 도넛 arc 한 조각(각도는 도, 0°=3시·시계방향; 12시 시작은 −90° 오프셋으로 재현). */
internal data class DonutArc(
    val startAngleDeg: Float,
    val sweepAngleDeg: Float,
    val color: Color,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val strokeWidth: Float,
)

/**
 * 심박존 분포 도넛.
 *
 * @param animateEntrance sweep 0→최종 등장 애니(12시부터 시계방향). iOS는 정적이라 기본 off(UX 패리티).
 * @param onSelectSegment 탭 시 **원본 data.segments 인덱스**, up/cancel 시 null. null이면 터치 비활성.
 */
@Composable
fun RDHeartRateZoneChart(
    data: DonutChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.defaults(isSystemInDarkTheme()),
    animateEntrance: Boolean = false,
    onSelectSegment: OnSelectSegment? = null,
) {
    val layout = remember(data) { DonutEngine.layout(data) }
    // sweep 등장 애니 — data 교체·animateEntrance 토글 시 0부터 재생(공용 헬퍼).
    val sweep by rememberEntranceProgress(data, animateEntrance, DONUT_SWEEP_DURATION_MS)

    // dp 의미의 링 폭·반경을 px로 환산(실기기 렌더/터치 붕괴 방지 — UX Critical-1).
    val density = LocalDensity.current.density
    val scaledStyle = remember(style, density) { style.scaledForDensity(density) }
    val ringPx = scaledStyle.donutRingWidth
    // 히트 대역은 시각 링보다 넓게 최소 48dp 확보(WCAG/Material 터치 타겟 — UX Major-3). 얇은 링에서도 관대.
    val hitBandPx = max(ringPx, MIN_HIT_TARGET_DP * density)

    // pointerInput 키에 콜백이 없으므로 최신 람다를 State로 유지 — 리컴포지션마다 새 람다를
    // 전달하는 호출자의 탭이 최초 캡처된 낡은 클로저로 통지되는 것을 막는다.
    val currentOnSelect by rememberUpdatedState(onSelectSegment)
    val gesture = if (onSelectSegment == null) {
        Modifier
    } else {
        Modifier.pointerInput(data, ringPx, hitBandPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                currentOnSelect?.invoke(
                    donutSegmentIndex(
                        px = down.position.x,
                        py = down.position.y,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        ringWidth = ringPx,
                        layout = layout,
                        hitBandWidth = hitBandPx,
                    ),
                )
                // up 또는 cancel(스크롤뷰 가로챔)로 모든 포인터가 떨어질 때까지 대기 → 해제 통지.
                do {
                    val event = awaitPointerEvent()
                } while (event.changes.any { it.pressed })
                currentOnSelect?.invoke(null)
            }
        }
    }

    // TalkBack 요약(UX Major-1): 존별 %를 낭독해 시각장애 사용자도 분포에 도달.
    val description = remember(layout) { donutDescription(layout) }
    Canvas(modifier.semantics { contentDescription = description }.then(gesture)) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        buildDonutArcs(layout, scaledStyle, size.width, size.height, sweep).forEach { drawDonutArc(it) }
    }
}

/** 심박존 도넛 TalkBack 요약(UX Major-1). 존별 퍼센트를 낭독. */
private fun donutDescription(layout: DonutChartLayout): String {
    if (layout.total <= 0.0 || layout.segments.isEmpty()) return "심박존 도넛, 데이터 없음"
    val zones = layout.segments.joinToString(", ") { seg ->
        "${seg.colorRole.name} ${(seg.sweepFraction * 100).roundToInt()}%"
    }
    return "심박존 분포 도넛. $zones"
}

/**
 * 도넛 레이아웃 → arc 리스트. total<=0/빈 세그먼트면 회색 빈 링 1개. 반경(=(min(w,h)-ring)/2)이
 * 0 이하면 빈 리스트(작은 bounds에서 음수 반경 방지 — iOS `guard radius > 0`).
 */
internal fun buildDonutArcs(
    layout: DonutChartLayout,
    style: ChartStyle,
    width: Float,
    height: Float,
    sweepProgress: Float = 1f,
): List<DonutArc> {
    val ring = style.donutRingWidth
    val radius = (min(width, height) - ring) / 2f
    if (radius <= 0f) return emptyList()
    val cx = width / 2f
    val cy = height / 2f
    val progress = sweepProgress.coerceIn(0f, 1f)

    if (layout.total <= 0.0 || layout.segments.isEmpty()) {
        return listOf(
            DonutArc(DONUT_START_DEG, FULL_CIRCLE_DEG, style.donutEmptyColor, cx, cy, radius, ring),
        )
    }
    return layout.segments.map { seg ->
        DonutArc(
            startAngleDeg = DONUT_START_DEG + FULL_CIRCLE_DEG * seg.startFraction.toFloat(),
            sweepAngleDeg = FULL_CIRCLE_DEG * seg.sweepFraction.toFloat() * progress,
            color = style.donutColors[seg.colorRole] ?: style.fallbackDataColor,
            centerX = cx,
            centerY = cy,
            radius = radius,
            strokeWidth = ring,
        )
    }
}

/**
 * 터치 좌표 → **원본 data.segments 인덱스**. 매칭 없으면 null.
 *
 * 반경 검사(링 대역 radius±hitBand/2)로 도넛 구멍·모서리 탭의 허위 선택을 막고, 각도→fraction→
 * 레이아웃 조각을 찾아 코어가 실어준 [DonutSegmentLayout.sourceIndex]를 그대로 보고한다
 * (value<=0 필터 규칙을 렌더러가 복제하지 않음 — 엔진 규칙 변경에 자동 추종).
 *
 * @param ringWidth 시각 링 폭(반경 계산 — 그리기와 일치해야 함). @param hitBandWidth 터치 판정 대역 폭(기본
 *   = [ringWidth]). 얇은 링에서도 관대한 탭을 위해 시각 링보다 넓게(최소 48dp) 줄 수 있다(UX Major-3).
 */
internal fun donutSegmentIndex(
    px: Float,
    py: Float,
    width: Float,
    height: Float,
    ringWidth: Float,
    layout: DonutChartLayout,
    hitBandWidth: Float = ringWidth,
): Int? {
    if (layout.total <= 0.0) return null
    val cx = width / 2f
    val cy = height / 2f
    val radius = (min(width, height) - ringWidth) / 2f
    if (radius <= 0f) return null

    val distance = hypot(px - cx, py - cy)
    val halfBand = hitBandWidth / 2f
    if (distance < radius - halfBand || distance > radius + halfBand) return null

    var angle = kotlin.math.atan2(py - cy, px - cx) + (Math.PI / 2).toFloat() // 12시 기준
    if (angle < 0f) angle += (2 * Math.PI).toFloat()
    val frac = angle / (2 * Math.PI).toFloat()

    val segment = layout.segments.firstOrNull {
        frac >= it.startFraction && frac < it.startFraction + it.sweepFraction
    } ?: return null
    return segment.sourceIndex.takeIf { it >= 0 }
}

private fun DrawScope.drawDonutArc(arc: DonutArc) {
    drawArc(
        color = arc.color,
        startAngle = arc.startAngleDeg,
        sweepAngle = arc.sweepAngleDeg,
        useCenter = false,
        topLeft = Offset(arc.centerX - arc.radius, arc.centerY - arc.radius),
        size = Size(arc.radius * 2f, arc.radius * 2f),
        style = Stroke(width = arc.strokeWidth, cap = StrokeCap.Butt), // butt 필수(round면 세그먼트 겹침)
    )
}

private const val DONUT_START_DEG = -90f       // 12시 시작(0°=3시 → −90°)
private const val FULL_CIRCLE_DEG = 360f
/** 최소 터치 타겟(dp) — WCAG/Material 권장. 얇은 링에서도 이만큼의 히트 대역을 확보(UX Major-3). */
private const val MIN_HIT_TARGET_DP = 48f

/** 도넛 arc 등장 애니 지속시간(ms). Material Emphasized. */
private const val DONUT_SWEEP_DURATION_MS = 550
