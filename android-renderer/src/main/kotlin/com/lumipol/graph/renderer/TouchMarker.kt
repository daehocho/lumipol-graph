// iOS: TouchMarker.swift
//
// 근접점 마커(수직선 + 시리즈별 점)를 조립하고 시리즈별 포맷 값을 반환한다. 근접 판정은 코어
// `LineChartEngine.nearest`(원본 도메인 x) — 양 플랫폼 동일. 순수 조립부([make]/[makeBackgroundOnly])는
// DrawScope 미의존이라 JVM 단위테스트로 검증 가능하고, 그리기는 [drawTouchMarker].
package com.lumipol.graph.renderer

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import com.lumipol.graph.LineChartEngine
import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.LineChartLayout
import com.lumipol.graph.model.NormalizedPoint
import com.lumipol.graph.model.SeriesRole

/**
 * 마커 조립 컨텍스트(iOS `TouchMarker.Context`).
 *
 * @param style 이미 밀도 환산된 스타일(터치 점 반경 등). @param density 스타일 밖 상수(터치선 폭)의 dp→px 환산.
 * @param axisBySeriesId,roleBySeriesId 첫 시리즈 우선 속성 맵. 스크럽은 60~120Hz로 호출되므로
 *   데이터가 불변인 동안 재계산하지 않게 호출부([LineChartInteraction])가 캐시본을 주입한다
 *   (기본값은 테스트 등 단발 호출용).
 */
internal data class TouchMarkerContext(
    val data: LineChartData,
    val layout: LineChartLayout,
    val style: ChartStyle,
    val plot: PlotArea,
    val formatter: (ChartAxis, Double) -> String,
    val density: Float = 1f,
    val axisBySeriesId: Map<String, Axis> = firstWinsBy(data) { it.axis },
    val roleBySeriesId: Map<String, SeriesRole> = firstWinsBy(data) { it.role },
)

/**
 * 마커 레이어 + seriesId→포맷값 + 스냅된 원본 도메인 x.
 * [snappedX]는 수직선 기준(첫 main 근접점) — 배경 area 보간에 사용.
 */
internal data class TouchMarkerResult(
    val layer: ContainerLayer,
    val valuesBySeriesId: Map<String, String>,
    val snappedX: Double,
)

internal object TouchMarker {

    /**
     * 원본 도메인 [rawX] 기준 마커. 표시 불가(플롯 없음·축 변환 불능·근접점 없음/전부 창밖)면 null.
     *
     * - 스냅 소스는 **main 시리즈** 근접점(없으면 첫 시리즈) — 고스트/오버레이는 성긴 샘플일 수 있음.
     * - 스냅/시리즈 근접점이 현재 표시 창 밖이면 마커/점·값을 생략. 단 도메인 양끝은 부동소수 반올림으로
     *   0/1을 [WINDOW_EPSILON] 이내 벗어날 수 있으므로 그 범위는 창 안으로 간주해 클램프한다.
     */
    fun make(rawX: Double, context: TouchMarkerContext): TouchMarkerResult? {
        if (!context.plot.isRenderable) return null
        val xTicks = ticks(ChartAxis.X, context.layout) ?: return null
        val xScale = AxisScale.from(xTicks) ?: return null

        // 창 안 점만 근접 후보로 — 창 밖 전역 최근접점이 스냅 소스가 되면 창 안 이웃이 있어도
        // 마커 전체가 null로 떨어진다(줌 가장자리). 경계는 WINDOW_EPSILON만큼 관대하게.
        val results = LineChartEngine.nearest(
            context.data,
            rawX,
            xMin = xScale.value(-WINDOW_EPSILON),
            xMax = xScale.value(1 + WINDOW_EPSILON),
        )
        val axisBySeriesId = context.axisBySeriesId
        val roleBySeriesId = context.roleBySeriesId

        val snapSource = results.firstOrNull { roleBySeriesId[it.seriesId] == SeriesRole.MAIN }
            ?: results.firstOrNull()
        val snappedX = snapSource?.x ?: return null

        val rawNx = xScale.position(snappedX)
        if (rawNx < -WINDOW_EPSILON || rawNx > 1 + WINDOW_EPSILON) return null
        val nx = rawNx.coerceIn(0.0, 1.0)

        val children = mutableListOf<LineChartLayer>()
        children.add(verticalLine(nx, context))

        val valuesBySeriesId = LinkedHashMap<String, String>()
        for (result in results) {
            // 창 밖 근접점은 점·값 모두 생략(짧은 고스트가 창 밖 값을 스크럽 위치인 양 보고 방지).
            val seriesNx = xScale.position(result.x)
            if (seriesNx < -WINDOW_EPSILON || seriesNx > 1 + WINDOW_EPSILON) continue

            if (roleBySeriesId[result.seriesId] == SeriesRole.OVERLAY) {
                // 오버레이: 축 없음 — 실값만 전달, 터치 점은 생략.
                valuesBySeriesId[result.seriesId] = context.formatter(ChartAxis.Y_OVERLAY, result.y)
                continue
            }
            val axis = axisBySeriesId[result.seriesId] ?: continue
            val chartAxis = chartAxis(axis) ?: continue
            val yTicks = ticks(chartAxis, context.layout) ?: continue
            val yScale = AxisScale.from(yTicks) ?: continue
            val point = context.plot.point(
                NormalizedPoint(x = nx, y = yScale.position(result.y)),
                axis,
            )
            val dotColor = when {
                roleBySeriesId[result.seriesId] == SeriesRole.GHOST -> context.style.ghostLineColor
                axis == Axis.SECONDARY -> context.style.secondaryLineColor
                else -> context.style.primaryLineColor
            }
            children.add(
                DotLayer(
                    name = "touch.dot.${result.seriesId}",
                    center = point,
                    radius = context.style.touchDotRadius,
                    color = dotColor,
                ),
            )
            valuesBySeriesId[result.seriesId] = context.formatter(chartAxis, result.y)
        }
        if (valuesBySeriesId.isEmpty()) return null
        return TouchMarkerResult(ContainerLayer("touch.marker", children), valuesBySeriesId, snappedX)
    }

    /**
     * 시리즈 없는 차트(배경 area 단독)용 마커 — 스냅 격자가 없으므로 [rawX]를 그대로 수직선 위치로
     * 쓴다(연속 스크럽). valuesBySeriesId는 빈 맵.
     */
    fun makeBackgroundOnly(rawX: Double, context: TouchMarkerContext): TouchMarkerResult? {
        if (!context.plot.isRenderable) return null
        val xTicks = ticks(ChartAxis.X, context.layout) ?: return null
        val xScale = AxisScale.from(xTicks) ?: return null
        val rawNx = xScale.position(rawX)
        if (rawNx < -WINDOW_EPSILON || rawNx > 1 + WINDOW_EPSILON) return null
        val nx = rawNx.coerceIn(0.0, 1.0)
        val container = ContainerLayer("touch.marker", listOf(verticalLine(nx, context)))
        return TouchMarkerResult(container, emptyMap(), xScale.value(nx))
    }

    private fun verticalLine(nx: Double, context: TouchMarkerContext): StrokeLayer {
        val lineX = context.plot.x(nx)
        return StrokeLayer(
            name = "touch.line",
            segments = listOf(
                listOf(
                    PlotPoint(lineX, context.plot.minY),
                    PlotPoint(lineX, context.plot.maxY),
                ),
            ),
            color = context.style.touchLineColor,
            width = TOUCH_LINE_WIDTH * context.density,
        )
    }

    private fun ticks(axis: ChartAxis, layout: LineChartLayout): List<com.lumipol.graph.model.AxisTick>? =
        layout.ticksFor(axis)

    private fun chartAxis(axis: Axis): ChartAxis? = when (axis) {
        Axis.PRIMARY -> ChartAxis.Y_PRIMARY
        Axis.SECONDARY -> ChartAxis.Y_SECONDARY
    }

    /** 창 경계 부동소수 흡수 epsilon(iOS `1e-9`). 엄격 비교로 바꾸면 끝-탭 침묵 드롭 회귀. */
    const val WINDOW_EPSILON = 1e-9
    private const val TOUCH_LINE_WIDTH = 1f
}

/** 마커(수직선 + 점) 그리기. 배치3 라인차트가 스크럽 상태에서 호출. */
internal fun DrawScope.drawTouchMarker(result: TouchMarkerResult, measurer: TextMeasurer) {
    render(result.layer, measurer)
}
