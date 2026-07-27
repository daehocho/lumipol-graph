// iOS: TouchMarker.swift
//
// 근접점 마커(수직선 + 시리즈별 점)를 조립하고 시리즈별 포맷 값을 반환한다. 창 필터·스냅 소스
// 선택·정규화 좌표는 코어 `LineChartEngine.nearestScrub`(B2)가 확정하고 — 양 플랫폼 동일 —
// 렌더러는 플랫폼 좌표 변환과 레이어 조립만 한다. 순수 조립부([make]/[makeBackgroundOnly])는
// DrawScope 미의존이라 JVM 단위테스트로 검증 가능하고, 그리기는 [drawTouchMarker].
package com.lumipol.graph.renderer

import com.lumipol.graph.ChartDefaults
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import com.lumipol.graph.LineChartEngine
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.LineChartLayout
import com.lumipol.graph.model.NormalizedPoint
import com.lumipol.graph.model.SeriesRole
import com.lumipol.graph.query.SCRUB_WINDOW_EPSILON

/**
 * 마커 조립 컨텍스트(iOS `TouchMarker.Context`).
 *
 * @param style 이미 밀도 환산된 스타일(터치 점 반경 등). @param density 스타일 밖 상수(터치선 폭)의 dp→px 환산.
 */
internal data class TouchMarkerContext(
    val data: LineChartData,
    val layout: LineChartLayout,
    val style: ChartStyle,
    val plot: PlotArea,
    val formatter: (ChartAxis, Double) -> String,
    val density: Float = 1f,
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
     * 스냅 규칙(main 우선·창 epsilon·오버레이 자체 정규화 y)은 코어 [LineChartEngine.nearestScrub] 소관.
     */
    fun make(rawX: Double, context: TouchMarkerContext): TouchMarkerResult? {
        if (!context.plot.isRenderable) return null
        val scrub = LineChartEngine.nearestScrub(context.data, context.layout, rawX) ?: return null

        val children = mutableListOf<LineChartLayer>()
        children.add(verticalLine(scrub.snappedNx, context))

        val valuesBySeriesId = LinkedHashMap<String, String>()
        for (p in scrub.perSeries) {
            valuesBySeriesId[p.seriesId] = context.formatter(p.chartAxis, p.y)
            val ny = p.ny ?: continue // 도트 불능(오버레이가 layout에 없음) — 값만 전달
            // 오버레이는 라인과 같은 반전 무시 매핑, 축 시리즈는 해당 축 매핑(코어 ny와 좌표계 일치).
            val center = if (p.role == SeriesRole.OVERLAY) {
                context.plot.pointIgnoringInversion(NormalizedPoint(x = p.nx, y = ny))
            } else {
                context.plot.point(NormalizedPoint(x = p.nx, y = ny), p.axis)
            }
            children.add(
                DotLayer(
                    name = "touch.dot.${p.seriesId}",
                    center = center,
                    radius = context.style.touchDotRadius,
                    // 라인·그라데이션과 같은 seriesColor 리졸버(맵 우선, 축/역할 폴백) — 도트만 다른 색이 되지 않게.
                    color = seriesColor(p.seriesId, p.role, p.axis, context.style),
                ),
            )
        }
        return TouchMarkerResult(ContainerLayer("touch.marker", children), valuesBySeriesId, scrub.snappedX)
    }

    /**
     * 시리즈 없는 차트(배경 area 단독)용 마커 — 스냅 격자가 없으므로 [rawX]를 그대로 수직선 위치로
     * 쓴다(연속 스크럽). valuesBySeriesId는 빈 맵.
     */
    fun makeBackgroundOnly(rawX: Double, context: TouchMarkerContext): TouchMarkerResult? {
        if (!context.plot.isRenderable) return null
        val xDomain = context.layout.domains.x
        if (xDomain.max <= xDomain.min) return null
        val rawNx = xDomain.normalize(rawX)
        if (rawNx < -SCRUB_WINDOW_EPSILON || rawNx > 1 + SCRUB_WINDOW_EPSILON) return null
        val nx = rawNx.coerceIn(0.0, 1.0)
        val container = ContainerLayer("touch.marker", listOf(verticalLine(nx, context)))
        return TouchMarkerResult(container, emptyMap(), xDomain.denormalize(nx))
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

    private val TOUCH_LINE_WIDTH = ChartDefaults.TOUCH_LINE_WIDTH.toFloat()
}

/** 마커(수직선 + 점) 그리기. 배치3 라인차트가 스크럽 상태에서 호출. */
internal fun DrawScope.drawTouchMarker(result: TouchMarkerResult, measurer: TextMeasurer) {
    render(result.layer, measurer)
}
