// iOS: RDBarChartView.swift
//
// 스플릿 막대 차트. 코어 BarChartLayout(정규화 완료)을 받아 그린다. 축·줌·터치 없음.
// iOS 비대칭(BarChartLayout 주입)을 스펙 1:1 원칙대로 유지한다. 순수 조립부([buildBarChartLayers])는
// JVM 단위테스트로 검증하고, composable [RDBarChart]는 그 결과를 Canvas로 렌더한다.
package com.lumipol.graph.renderer

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.lumipol.graph.model.BarChartLayout
import com.lumipol.graph.model.BarColorRole
import com.lumipol.graph.query.barIndexAtX
import com.lumipol.graph.query.isLabelVisible
import com.lumipol.graph.query.labelStride
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 스플릿 막대 차트.
 *
 * @param layout 앱이 `BarChartEngine.layout`으로 만든 정규화 레이아웃(iOS 비대칭 계약 유지).
 * @param barLabels 막대 값 라벨(표시 페이스 등). 정적 표시는 하지 않고 TalkBack 요약·롱프레스 말풍선
 *   값 소스로만 쓰인다. null이면 값 없음.
 * @param xAxisLabels 막대 아래 구간 라벨. null이면 생략.
 * @param yLabelFormatter y틱 값 포매터. null이면 정수 반올림.
 * @param animateEntrance baseline→값 높이 성장 애니. iOS는 정적이라 기본 off(UX 패리티).
 * @param onSelectedIndexChange 롱프레스 스크럽 선택 인덱스 변경 통지(해제 시 null). null이면 미통지.
 */
@Composable
fun RDBarChart(
    layout: BarChartLayout,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.defaults(isSystemInDarkTheme()),
    barLabels: List<String>? = null,
    xAxisLabels: List<String>? = null,
    yLabelFormatter: ((Double) -> String)? = null,
    animateEntrance: Boolean = false,
    onSelectedIndexChange: ((Int?) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    // 성장 등장 애니 — layout 교체·animateEntrance 토글 시 0부터 재생(공용 헬퍼).
    val growth by rememberEntranceProgress(layout, animateEntrance, BAR_GROWTH_DURATION_MS)
    // dp 의미의 스타일 기하 값을 px로 환산(실기기 렌더 붕괴 방지 — UX Critical-1).
    val density = LocalDensity.current.density
    val fontScale = LocalDensity.current.fontScale
    val scaledStyle = remember(style, density) { style.scaledForDensity(density) }
    // 라벨 최대 폭은 layout/스타일/글꼴배율이 바뀔 때만 측정(등장 애니로 growth가 매 프레임 바뀌어도
    // 재측정하지 않도록 그리기 경로 밖에서 memoize — 리뷰 #3). stride 자체는 폭·크기로 싸게 계산.
    val xLabelWidthPx = remember(xAxisLabels, scaledStyle, fontScale, measurer) {
        maxLabelWidthPx(measurer, xAxisLabels, scaledStyle, fontScale)
    }
    // TalkBack 요약(UX Major-1): 캔버스는 불투명하므로 컨테이너에 막대 수·값 요약을 노출한다.
    val description = remember(layout, barLabels) { barChartDescription(layout, barLabels) }

    // 롱프레스 스크럽 선택 상태. layout이 바뀌면 initial 리멤버 값은 이미 null이지만, 재사용되는 remember
    // 슬롯은 layout 키를 두지 않는다 — 리셋 통지는 아래 LaunchedEffect(layout)가 명시적으로 담당한다
    // (리뷰 #1: layout 교체 시 onSelectedIndexChange(null) 미발화로 부모 미러가 stale해지는 계약 갭 방지).
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current
    // pointerInput 키에 콜백이 없으므로 최신 람다를 State로 유지(리뷰 #2: stale 클로저로 통지되는 것을 막음).
    val latestOnChange by rememberUpdatedState(onSelectedIndexChange)
    fun setSelection(idx: Int?, haptic: Boolean) {
        if (idx == selectedIndex) return
        selectedIndex = idx
        if (idx != null && haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        latestOnChange?.invoke(idx)
    }
    // layout 교체 시 선택을 리셋하고 명시적으로 통지(iOS render()의 선택 리셋 대응). 첫 컴포지션은
    // selectedIndex가 이미 null이라 setSelection의 no-op 가드로 무발화.
    LaunchedEffect(layout) { setSelection(null, haptic = false) }
    val gestureModifier = if (!barLabels.isNullOrEmpty()) {
        Modifier.pointerInput(layout, barLabels, scaledStyle) {
            // plot은 scrub 호출마다 현재 size로 재계산한다(리뷰 #3: 코루틴 시작 시 1회 캡처하면
            // layout/barLabels/scaledStyle이 불변인 채 리사이즈(회전)만 일어날 때 stale plot으로
            // 히트테스트한다).
            fun scrub(px: Float) {
                val plot = PlotArea(size.width.toDouble(), size.height.toDouble(), scaledStyle.plotInsets)
                setSelection(barIndexAtX(px.toDouble(), plot.minX, plot.width, layout.bars.size), haptic = true)
            }
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> scrub(offset.x) },
                onDrag = { change, _ -> change.consume(); scrub(change.position.x) },
                onDragEnd = { setSelection(null, haptic = false) },
                onDragCancel = { setSelection(null, haptic = false) },
            )
        }
    } else {
        Modifier
    }

    // 선택 막대의 말풍선 텍스트 크기는 Canvas 밖에서 memoize(리뷰 #4: draw 람다 안 매 프레임 측정 제거).
    // 범위 밖(layout 교체 중 1프레임) 인덱스는 null로 가드해 #1의 sel 가드와 정합시킨다.
    val selectedLabel = selectedIndex?.takeIf { it < layout.bars.size }?.let { barLabels?.getOrNull(it) }
    val calloutSize = remember(selectedLabel, scaledStyle.barCalloutFontSize, scaledStyle.barCalloutFontWeight, measurer) {
        selectedLabel?.let { lbl ->
            measurer.measure(
                lbl,
                TextStyle(fontSize = scaledStyle.barCalloutFontSize.sp, fontWeight = scaledStyle.barCalloutFontWeight),
            ).size
        }
    }

    Canvas(modifier.semantics { contentDescription = description }.then(gestureModifier)) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        var layers = buildBarChartLayers(
            layout = layout,
            style = scaledStyle,
            sizeWidth = size.width.toDouble(),
            sizeHeight = size.height.toDouble(),
            xAxisLabels = xAxisLabels,
            yLabelFormatter = yLabelFormatter,
            growth = growth,
            density = density,
            xLabelWidthPx = xLabelWidthPx,
        )
        // layout 교체 직후 selectedIndex가 새(더 짧은) layout 범위를 벗어난 1프레임을 방지(리뷰 #1).
        val sel = selectedIndex?.takeIf { it < layout.bars.size }
        if (sel != null) {
            layers = applyBarSelection(
                layers, layout, scaledStyle,
                sizeWidth = size.width.toDouble(), sizeHeight = size.height.toDouble(),
                selectedIndex = sel, barLabels = barLabels,
                calloutTextWidthPx = (calloutSize?.width ?: 0).toDouble(),
                calloutTextHeightPx = (calloutSize?.height ?: 0).toDouble(),
                density = density,
            )
        }
        layers.forEach { render(it, measurer) }
    }
}

/** 막대 차트 TalkBack 요약 문자열(UX Major-1). 라벨이 있으면 구간별 값을 함께 낭독. */
private fun barChartDescription(layout: BarChartLayout, barLabels: List<String>?): String {
    if (layout.bars.isEmpty()) return "막대 차트, 데이터 없음"
    val detail = barLabels?.takeIf { it.isNotEmpty() }
        ?.let { labels -> layout.bars.indices.joinToString(", ") { "구간 ${it + 1} ${labels.getOrNull(it) ?: ""}".trim() } }
    return "막대 차트, 구간 ${layout.bars.size}개" + (detail?.let { ". $it" } ?: "")
}

/** 라벨 목록 중 가장 넓은 폭(px). 비었으면 0. 솎아내기 stride 입력(리뷰 #3: 그리기 밖에서 1회 측정·memoize). */
private fun maxLabelWidthPx(
    measurer: TextMeasurer,
    labels: List<String>?,
    style: ChartStyle,
    fontScale: Float,
): Double = labels?.maxOfOrNull {
    measureLabelWidthPx(
        measurer = measurer,
        text = it,
        fontSizeSp = style.axisLabelFontSize,
        fontFamily = style.axisLabelFontFamily,
        fontWeight = style.axisLabelFontWeight,
        fontScale = fontScale,
    )
} ?: 0.0

/**
 * BarChartLayout → z-순서 레이어(그리드/틱라벨 → 막대 → x라벨 → 참조선). 막대 위 정적 값 라벨은
 * 없음(iOS: 값은 롱프레스 말풍선으로만 노출).
 * 막대가 없거나 렌더 불가 플롯이면 빈 리스트(iOS `guard !bars.isEmpty, plot>0`).
 */
internal fun buildBarChartLayers(
    layout: BarChartLayout,
    style: ChartStyle,
    sizeWidth: Double,
    sizeHeight: Double,
    xAxisLabels: List<String>? = null,
    yLabelFormatter: ((Double) -> String)? = null,
    growth: Float = 1f,
    density: Float = 1f,
    xLabelWidthPx: Double = 0.0,
): List<LineChartLayer> {
    if (layout.bars.isEmpty()) return emptyList()
    val plot = PlotArea(sizeWidth, sizeHeight, style.plotInsets)
    if (!plot.isRenderable) return emptyList()

    val layers = mutableListOf<LineChartLayer>()
    val n = layout.bars.size

    // 라벨 솎아내기 stride(장거리·하프 등 슬롯보다 넓은 라벨 겹침 방지). 라벨 최대 폭은 composable이 미리
    // 측정해 주입한다(그리기 경로에서 프레임마다 재측정하지 않도록). 표시 여부는 isLabelVisible(첫·마지막
    // 항상 표시).
    val gap = BAR_LABEL_MIN_GAP * density
    val xLabelStride = if (xAxisLabels.isNullOrEmpty()) 1 else labelStride(n, plot.width, xLabelWidthPx, gap)

    // Y 그리드 + 틱 라벨
    for ((i, tick) in layout.yTicks.withIndex()) {
        val y = plot.maxY - tick.position * plot.height
        style.gridLineColor?.let { grid ->
            layers.add(
                StrokeLayer(
                    name = "barGrid.$i",
                    segments = listOf(listOf(PlotPoint(plot.minX, y), PlotPoint(plot.maxX, y))),
                    color = grid,
                    width = style.gridLineWidth,
                    dash = style.gridLineDashPattern,
                ),
            )
        }
        if (style.barShowYAxisLabels) {
            val text = yLabelFormatter?.invoke(tick.value) ?: tick.value.roundToInt().toString()
            layers.add(
                TextLayer(
                    name = "barYLabel.$i",
                    text = text,
                    anchorX = plot.minX - BAR_LABEL_GAP * density,
                    anchorY = y,
                    hAlign = HAlign.RIGHT,
                    vAlign = VAlign.CENTER,
                    color = style.axisLabelColor,
                    fontSizeSp = style.axisLabelFontSize,
                    fontFamily = style.axisLabelFontFamily,
                    fontWeight = style.axisLabelFontWeight,
                ),
            )
        }
    }

    // 연속 색상 앵커(이 런 막대 value 기준). average = 등거리 스플릿에서 런 평균 페이스와 일치.
    // 온전한 스플릿만 사용해 부분 스플릿(짧은 잔여) 이상치가 팔레트를 왜곡하지 않게 한다.
    // 단 온전 스플릿 2개 미만이거나 값 범위가 없으면 전체 막대로 폴백(색 신호 보존).
    val fullValues = layout.bars.filter { !it.isPartial }.map { it.value }
    val fullHasRange = fullValues.size >= 2 && fullValues.max() > fullValues.min()
    val anchorValues = if (fullHasRange) fullValues else layout.bars.map { it.value }
    val fastest = anchorValues.min()
    val slowest = anchorValues.max()
    val average = anchorValues.sum() / anchorValues.size

    // 막대
    val slot = plot.width / n
    val barWidth = slot * style.barWidthRatio
    for ((i, bar) in layout.bars.withIndex()) {
        val fullH = min(max(style.barMinHeight.toDouble(), bar.heightFraction * plot.height), plot.height)
        val h = fullH * growth.coerceIn(0f, 1f)
        val x = plot.minX + slot * i + (slot - barWidth) / 2
        val colorInput = BarPaceColorInput(
            value = bar.value, fastest = fastest, slowest = slowest, average = average,
            isPartial = bar.isPartial, index = i, colorRole = bar.colorRole,
        )
        val barColor = style.barColorProvider?.invoke(colorInput) ?: ChartStyle.defaultPaceColor(colorInput)
        layers.add(
            RectLayer(
                name = "bar.$i",
                minX = x,
                minY = plot.maxY - h,
                width = barWidth,
                height = h,
                color = barColor,
                cornerRadius = style.barCornerRadius,
                alpha = if (bar.isPartial) style.partialBarAlpha else 1f,
            ),
        )
        val midX = x + barWidth / 2
        if (style.barShowXAxisLabels && isLabelVisible(i, n, xLabelStride)) {
            xAxisLabels?.getOrNull(i)?.let { label ->
                layers.add(
                    TextLayer(
                        name = "barXLabel.$i",
                        text = label,
                        anchorX = midX,
                        anchorY = plot.maxY + BAR_X_LABEL_GAP * density,
                        hAlign = HAlign.CENTER,
                        vAlign = VAlign.BELOW,
                        color = style.axisLabelColor,
                        fontSizeSp = style.axisLabelFontSize,
                        fontFamily = style.axisLabelFontFamily,
                        fontWeight = style.axisLabelFontWeight,
                    ),
                )
            }
        }
    }

    // 참조선(목표/평균)
    layout.referenceLinePosition?.let { refPos ->
        val y = plot.maxY - refPos * plot.height
        layers.add(
            StrokeLayer(
                name = "barRefLine",
                segments = listOf(listOf(PlotPoint(plot.minX, y), PlotPoint(plot.maxX, y))),
                color = style.barReferenceLineColor,
                width = 1f * density,
                dash = style.refLineDashPattern,
            ),
        )
    }
    return layers
}

/**
 * [buildBarChartLayers] 결과에 롱프레스 선택 상태를 반영한다(순수 — iOS `applySelection` 대응).
 * 미선택 막대는 [ChartStyle.barDimOpacity] 배율로 흐리고, 선택 막대 중앙에 세로 가이드선을,
 * 그 위에 페이스 말풍선을 얹는다. 말풍선은 막대 높이와 무관하게 **항상 플롯 상단**에 고정(짧은
 * 막대에서 손가락 가림 방지)하고 좌우로 클램프한다. 말풍선 텍스트 크기는 composable이 측정해 넘긴다.
 */
internal fun applyBarSelection(
    layers: List<LineChartLayer>,
    layout: BarChartLayout,
    style: ChartStyle,
    sizeWidth: Double,
    sizeHeight: Double,
    selectedIndex: Int,
    barLabels: List<String>?,
    calloutTextWidthPx: Double,
    calloutTextHeightPx: Double,
    density: Float = 1f,
): List<LineChartLayer> {
    val plot = PlotArea(sizeWidth, sizeHeight, style.plotInsets)
    if (!plot.isRenderable) return layers
    val selName = "bar.$selectedIndex"
    val selRect = layers.filterIsInstance<RectLayer>().firstOrNull { it.name == selName } ?: return layers

    // 1) 미선택 막대 dim(선택 막대는 base alpha 유지).
    val dimmed = layers.map { layer ->
        if (layer is RectLayer && layer.name.startsWith("bar.") && layer.name != selName) {
            layer.copy(alpha = layer.alpha * style.barDimOpacity)
        } else {
            layer
        }
    }.toMutableList()

    val midX = selRect.minX + selRect.width / 2

    // 2) 세로 가이드선(플롯 상단~하단).
    dimmed.add(
        StrokeLayer(
            name = "bar.selection.line",
            segments = listOf(listOf(PlotPoint(midX, plot.minY), PlotPoint(midX, plot.maxY))),
            color = style.barSelectionLineColor,
            width = 1f * density,
        ),
    )

    // 3) 말풍선(페이스만) — barLabels 있을 때만. 항상 플롯 상단 고정 + 좌우 클램프.
    val label = barLabels?.getOrNull(selectedIndex)
    if (label != null && calloutTextWidthPx > 0) {
        val padH = BAR_CALLOUT_PAD_H * density
        val padV = BAR_CALLOUT_PAD_V * density
        val bw = calloutTextWidthPx + padH * 2
        val bh = calloutTextHeightPx + padV * 2
        // iOS 클램프: min 먼저(우측), max 나중(좌측 우선) — coerceIn(min>max) 예외 회피.
        var bx = midX - bw / 2
        bx = minOf(bx, plot.maxX - bw)
        bx = maxOf(plot.minX, bx)
        val by = plot.minY
        dimmed.add(
            RectLayer(
                name = "bar.selection.bubble",
                minX = bx, minY = by, width = bw, height = bh,
                color = style.barCalloutBackgroundColor,
                cornerRadius = BAR_CALLOUT_CORNER * density,
            ),
        )
        dimmed.add(
            TextLayer(
                name = "bar.selection.text",
                text = label,
                anchorX = bx + padH,
                anchorY = by + padV,
                hAlign = HAlign.LEFT,
                vAlign = VAlign.BELOW, // 상단이 앵커
                color = style.barCalloutTextColor,
                fontSizeSp = style.barCalloutFontSize,
                fontWeight = style.barCalloutFontWeight,
            ),
        )
    }
    return dimmed
}

private const val BAR_LABEL_GAP = 4.0               // y틱 라벨과 축 사이(iOS insets.left-4)
private const val BAR_X_LABEL_GAP = 4.0             // x축 라벨과 막대 바닥 사이(iOS maxY+4)
private const val BAR_LABEL_MIN_GAP = 6.0           // 솎아낸 이웃 라벨 사이 최소 여백(dp) — 겹침 방지
private const val BAR_CALLOUT_PAD_H = 8.0           // 선택 말풍선 좌우 내부 여백(dp)
private const val BAR_CALLOUT_PAD_V = 4.0           // 선택 말풍선 상하 내부 여백(dp)
private const val BAR_CALLOUT_CORNER = 6f           // 선택 말풍선 모서리 반경(dp)

/** 바 성장 등장 애니 지속시간(ms). Material Emphasized. */
private const val BAR_GROWTH_DURATION_MS = 300
/** Material Emphasized Decelerate 이징(라인/바/도넛 등장 공용). */
internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
