// iOS: RDBarChartView.swift
//
// 스플릿 막대 차트. 코어 BarChartLayout(정규화 완료)을 받아 그린다. 축·줌·터치 없음.
// iOS 비대칭(BarChartLayout 주입)을 스펙 1:1 원칙대로 유지한다. 순수 조립부([buildBarChartLayers])는
// JVM 단위테스트로 검증하고, composable [RDBarChart]는 그 결과를 Canvas로 렌더한다.
package com.lumipol.graph.renderer

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.lumipol.graph.model.BarChartLayout
import com.lumipol.graph.model.BarColorRole
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 스플릿 막대 차트.
 *
 * @param layout 앱이 `BarChartEngine.layout`으로 만든 정규화 레이아웃(iOS 비대칭 계약 유지).
 * @param barLabels 막대 위 라벨(표시 페이스 등). null이면 생략.
 * @param xAxisLabels 막대 아래 구간 라벨. null이면 생략.
 * @param yLabelFormatter y틱 값 포매터. null이면 정수 반올림.
 * @param animateEntrance baseline→값 높이 성장 애니. iOS는 정적이라 기본 off(UX 패리티).
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
) {
    val measurer = rememberTextMeasurer()
    // 성장 등장 애니 — layout 교체·animateEntrance 토글 시 0부터 재생(공용 헬퍼).
    val growth by rememberEntranceProgress(layout, animateEntrance, BAR_GROWTH_DURATION_MS)
    // dp 의미의 스타일 기하 값을 px로 환산(실기기 렌더 붕괴 방지 — UX Critical-1).
    val density = LocalDensity.current.density
    val scaledStyle = remember(style, density) { style.scaledForDensity(density) }
    // TalkBack 요약(UX Major-1): 캔버스는 불투명하므로 컨테이너에 막대 수·값 요약을 노출한다.
    val description = remember(layout, barLabels) { barChartDescription(layout, barLabels) }
    Canvas(modifier.semantics { contentDescription = description }) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        buildBarChartLayers(
            layout = layout,
            style = scaledStyle,
            sizeWidth = size.width.toDouble(),
            sizeHeight = size.height.toDouble(),
            barLabels = barLabels,
            xAxisLabels = xAxisLabels,
            yLabelFormatter = yLabelFormatter,
            growth = growth,
            density = density,
        ).forEach { render(it, measurer) }
    }
}

/** 막대 차트 TalkBack 요약 문자열(UX Major-1). 라벨이 있으면 구간별 값을 함께 낭독. */
private fun barChartDescription(layout: BarChartLayout, barLabels: List<String>?): String {
    if (layout.bars.isEmpty()) return "막대 차트, 데이터 없음"
    val detail = barLabels?.takeIf { it.isNotEmpty() }
        ?.let { labels -> layout.bars.indices.joinToString(", ") { "구간 ${it + 1} ${labels.getOrNull(it) ?: ""}".trim() } }
    return "막대 차트, 구간 ${layout.bars.size}개" + (detail?.let { ". $it" } ?: "")
}

/**
 * BarChartLayout → z-순서 레이어(그리드/틱라벨 → 막대 → 막대라벨 → x라벨 → 참조선).
 * 막대가 없거나 렌더 불가 플롯이면 빈 리스트(iOS `guard !bars.isEmpty, plot>0`).
 */
internal fun buildBarChartLayers(
    layout: BarChartLayout,
    style: ChartStyle,
    sizeWidth: Double,
    sizeHeight: Double,
    barLabels: List<String>? = null,
    xAxisLabels: List<String>? = null,
    yLabelFormatter: ((Double) -> String)? = null,
    growth: Float = 1f,
    density: Float = 1f,
): List<LineChartLayer> {
    if (layout.bars.isEmpty()) return emptyList()
    val plot = PlotArea(sizeWidth, sizeHeight, style.plotInsets)
    if (!plot.isRenderable) return emptyList()

    val layers = mutableListOf<LineChartLayer>()

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
                ),
            )
        }
    }

    // 막대
    val n = layout.bars.size
    val slot = plot.width / n
    val barWidth = slot * style.barWidthRatio
    for ((i, bar) in layout.bars.withIndex()) {
        val fullH = min(max(style.barMinHeight.toDouble(), bar.heightFraction * plot.height), plot.height)
        val h = fullH * growth.coerceIn(0f, 1f)
        val x = plot.minX + slot * i + (slot - barWidth) / 2
        layers.add(
            RectLayer(
                name = "bar.$i",
                minX = x,
                minY = plot.maxY - h,
                width = barWidth,
                height = h,
                color = style.barColors[bar.colorRole] ?: style.fallbackDataColor,
                cornerRadius = style.barCornerRadius,
                alpha = if (bar.isPartial) style.partialBarAlpha else 1f,
            ),
        )
        val midX = x + barWidth / 2
        barLabels?.getOrNull(i)?.let { label ->
            layers.add(
                TextLayer(
                    name = "barLabel.$i",
                    text = label,
                    anchorX = midX,
                    anchorY = (plot.maxY - h) - BAR_LABEL_ABOVE_GAP * density,
                    hAlign = HAlign.CENTER,
                    vAlign = VAlign.ABOVE,
                    color = style.axisLabelColor,
                    fontSizeSp = style.axisLabelFontSize,
                ),
            )
        }
        if (style.barShowXAxisLabels) {
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

private const val BAR_LABEL_GAP = 4.0               // y틱 라벨과 축 사이(iOS insets.left-4)
private const val BAR_LABEL_ABOVE_GAP = 2.0         // 막대 위 라벨 여백(iOS rect.minY-2)
private const val BAR_X_LABEL_GAP = 4.0             // x축 라벨과 막대 바닥 사이(iOS maxY+4)

/** 바 성장 등장 애니 지속시간(ms). Material Emphasized. */
private const val BAR_GROWTH_DURATION_MS = 300
/** Material Emphasized Decelerate 이징(라인/바/도넛 등장 공용). */
internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
