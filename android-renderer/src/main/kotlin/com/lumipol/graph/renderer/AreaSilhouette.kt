// iOS: AreaSilhouette.swift
package com.lumipol.graph.renderer

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import com.lumipol.graph.model.Point

/**
 * 페이스 라인 뒤에 깔리는 고도 실루엣(장식).
 *
 * 축·스크럽과 무관한 순수 프레젠테이션 레이어. 순수 계산부([heightFractions])와 Path 조립부
 * ([build])는 DrawScope 미의존이라 JVM 단위테스트로 검증 가능하고, 그리기는 [drawAreaSilhouette].
 *
 * area 입력 타입은 로컬 `AreaPoint`를 신설하지 않고 core `Point`(`com.lumipol.graph.model.Point`)를
 * 재사용한다(arch 계약: 공개 입력 타입 중복 제거). [points]는 x 오름차순 전제(보간 이진탐색과 동일).
 */
internal object AreaSilhouette {
    /**
     * 값들을 자체 min~max로 0~1 정규화 — 코어 질의(`heightFractions`)에 위임(interpolatedY와 같은
     * 이관 사유: 플랫폼 중립 수학). 축퇴(전부 동일) 시 모두 0(평지) — `AxisDomain.normalize`(0.5)와
     * 다른 실루엣 전용 의미론이므로 그쪽으로 대체하면 안 된다(iOS parity).
     * [minSpan]은 노이즈가 산맥으로 보이지 않게 하는 분모 하한(`ChartStyle.areaMinValueSpan`).
     */
    fun heightFractions(values: List<Double>, minSpan: Double = 0.0): List<Double> =
        com.lumipol.graph.query.heightFractions(values, minSpan)

    /**
     * 도메인 area 포인트 → 실루엣 채움 폴리곤. 2점 미만이거나 렌더 불가 플롯이면 null.
     *
     * x는 [xScale]로 정규화 위치 산출 후 [PlotArea.x]. y는 heightFraction × areaHeightFraction,
     * 바닥(maxY) 기준 위로. 축 반전 무관(자체 매핑) — [PlotArea.y]는 쓰지 않는다.
     * 시리즈 x-도메인보다 넓은 area는 정규화 위치가 0~1 밖이 되므로 좌표를 플롯 영역으로 클램프한다.
     */
    fun build(
        points: List<Point>,
        xScale: AxisScale,
        plot: PlotArea,
        style: ChartStyle,
    ): AreaFillLayer? {
        if (points.size < 2 || !plot.isRenderable) return null
        val fractions = heightFractions(points.map { it.y }, style.areaMinValueSpan)
        val baseY = plot.maxY
        val usableHeight = style.areaHeightFraction * plot.height

        fun pixel(index: Int): PlotPoint {
            val nx = xScale.position(points[index].x).coerceIn(0.0, 1.0)
            return PlotPoint(x = plot.x(nx), y = baseY - fractions[index] * usableHeight)
        }

        val polygon = buildList {
            add(pixel(0))
            for (i in 1 until points.size) add(pixel(i))
            add(PlotPoint(pixel(points.size - 1).x, baseY))
            add(PlotPoint(pixel(0).x, baseY))
        }
        return AreaFillLayer(name = "area.altitude", polygon = polygon, color = style.areaFillColor)
    }
}

/** 배경 고도 실루엣 그리기(최하단). 렌더 불가/2점 미만이면 아무것도 안 그림. */
internal fun DrawScope.drawAreaSilhouette(
    points: List<Point>,
    xScale: AxisScale,
    plot: PlotArea,
    style: ChartStyle,
    measurer: TextMeasurer,
) {
    AreaSilhouette.build(points, xScale, plot, style)?.let { render(it, measurer) }
}
