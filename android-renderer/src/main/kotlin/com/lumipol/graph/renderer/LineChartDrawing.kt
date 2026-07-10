// iOS: ChartLayerBuilder.swift
//
// iOS는 CALayer 트리를 조립했지만 Compose엔 유지 레이어가 없다 → z-순서 = 그리기 함수 호출 순서.
// 다만 iOS 테스트가 "레이어 트리(이름·순서·기하)"를 검증하므로, 픽셀 그리기 전에 **순수 중간 모델**
// [LineChartLayer] 리스트를 만들어 검증 가능성을 유지하고([buildLineChartLayers]),
// [drawLineChart]는 그 모델을 `DrawScope`로 렌더한다. (arch 계약 6 대응)
package com.lumipol.graph.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.LineChartLayout
import com.lumipol.graph.model.NormalizedPoint
import com.lumipol.graph.model.SeriesRole

// =====================================================================================
// 순수 중간 모델 — DrawScope 없이 조립·검증 가능(Path/DrawScope 미의존, Color 등 값 타입만).
// =====================================================================================

/** 텍스트 정렬(측정된 폭·높이 기준 앵커 오프셋 계산용). */
internal enum class HAlign { LEFT, CENTER, RIGHT }
internal enum class VAlign { ABOVE, BELOW, CENTER } // ABOVE=텍스트 하단이 앵커, BELOW=상단이 앵커

/** 그리기 요소 한 개. [name]은 iOS CALayer 이름과 1:1(테스트가 트리 이름·순서를 검증). */
internal sealed interface LineChartLayer {
    val name: String
}

/**
 * 폴리라인 스트로크(시리즈/그리드/마커선/기준선/터치선). [segments]는 여러 부분경로(그리드는 선분 다발).
 * [dash]가 있으면 점선, [trimmable]이면 등장 애니 진행률로 잘라 그린다(main 시리즈).
 */
internal data class StrokeLayer(
    override val name: String,
    val segments: List<List<PlotPoint>>,
    val color: Color,
    val width: Float,
    val dash: FloatArray? = null,
    val cap: StrokeCap = StrokeCap.Butt,
    val join: StrokeJoin = StrokeJoin.Miter,
    val trimmable: Boolean = false,
) : LineChartLayer

/** 사각형 채움(밴드/막대). [alpha]는 부분 스플릿 막대 흐림(0.6)에 사용. */
internal data class RectLayer(
    override val name: String,
    val minX: Double,
    val minY: Double,
    val width: Double,
    val height: Double,
    val color: Color,
    val cornerRadius: Float = 0f,
    val alpha: Float = 1f,
) : LineChartLayer

/** 원(터치 점). */
internal data class DotLayer(
    override val name: String,
    val center: PlotPoint,
    val radius: Float,
    val color: Color,
) : LineChartLayer

/**
 * 라인 아래 area 그라데이션(primary main 시리즈). [polygon]은 라인+바닥으로 닫힌 채움 영역,
 * 그라데이션은 [topY](=플롯 상단, 알파 max)→[bottomY](=플롯 하단, 알파 0)의 수직 방향.
 */
internal data class GradientLayer(
    override val name: String,
    val polygon: List<PlotPoint>,
    val topColor: Color,
    val bottomColor: Color,
    val topY: Double,
    val bottomY: Double,
) : LineChartLayer

/** 채움 폴리곤(배경 고도 실루엣). iOS `area.altitude`. */
internal data class AreaFillLayer(
    override val name: String,
    val polygon: List<PlotPoint>,
    val color: Color,
) : LineChartLayer

/** 정렬·앵커를 가진 텍스트(축/마커/기준선 라벨). 실제 위치는 측정 후 draw 시 확정. */
internal data class TextLayer(
    override val name: String,
    val text: String,
    val anchorX: Double,
    val anchorY: Double,
    val hAlign: HAlign,
    val vAlign: VAlign,
    val color: Color,
    val fontSizeSp: Float,
) : LineChartLayer

/** 컨테이너(마커·기준선·축라벨 묶음). iOS 컨테이너 CALayer 대응. */
internal data class ContainerLayer(
    override val name: String,
    val children: List<LineChartLayer>,
) : LineChartLayer

// =====================================================================================
// 순수 조립부 — iOS ChartLayerBuilder.build 대응.
// =====================================================================================

/**
 * 코어 [LineChartLayout]을 z-순서 [LineChartLayer] 리스트로 조립한다(그리드→밴드→마커→고스트→
 * 그라데이션+main→오버레이→기준선→축라벨). 렌더 불가 플롯이면 빈 리스트.
 *
 * 코어 API가 시리즈 id 유일성을 강제하지 않으므로 중복 시 **첫 시리즈 우선**으로 축을 해석한다
 * (Kotlin `associate`는 마지막 우선이라 `putIfAbsent`로 명시).
 */
internal fun buildLineChartLayers(
    layout: LineChartLayout,
    data: LineChartData,
    style: ChartStyle,
    plot: PlotArea,
    formatter: (ChartAxis, Double) -> String,
    density: Float = 1f,
): List<LineChartLayer> {
    if (!plot.isRenderable) return emptyList()

    val axisBySeriesId = firstWinsAxis(data)
    val layers = mutableListOf<LineChartLayer>()

    style.gridLineColor?.let { gridColor ->
        gridLayer(layout, gridColor, style, plot)?.let(layers::add)
    }
    layout.refBands.forEachIndexed { index, band ->
        val y1 = plot.y(band.lower, band.axis)
        val y2 = plot.y(band.upper, band.axis)
        layers.add(
            RectLayer(
                name = "band.$index",
                minX = plot.minX,
                minY = minOf(y1, y2),
                width = plot.width,
                height = kotlin.math.abs(y1 - y2),
                color = style.refBandColor,
            ),
        )
    }
    layout.markers.forEachIndexed { index, marker ->
        layers.add(markerLayer(marker, index, style, plot, density))
    }
    layout.series.filter { it.role == SeriesRole.GHOST }.forEach { series ->
        val axis = axisBySeriesId[series.id] ?: Axis.PRIMARY
        val points = linePoints(series.points, axis, plot) ?: return@forEach
        layers.add(
            StrokeLayer(
                name = "series.ghost.${series.id}",
                segments = listOf(points),
                color = style.ghostLineColor,
                width = style.ghostLineWidth,
                dash = style.ghostDashPattern,
                join = StrokeJoin.Round,
            ),
        )
    }
    layout.series.filter { it.role == SeriesRole.MAIN }.forEach { series ->
        val axis = axisBySeriesId[series.id] ?: Axis.PRIMARY
        val points = linePoints(series.points, axis, plot) ?: return@forEach
        if (style.gradientMaxAlpha > 0f && axis == Axis.PRIMARY) {
            layers.add(gradientLayer(series.id, points, axis, style, plot))
        }
        layers.add(
            StrokeLayer(
                name = "series.main.${series.id}",
                segments = listOf(points),
                color = lineColor(axis, style),
                width = style.lineWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                trimmable = true,
            ),
        )
    }
    layout.series.filter { it.role == SeriesRole.OVERLAY }.forEach { series ->
        val points = overlayLinePoints(series.points, plot) ?: return@forEach
        layers.add(
            StrokeLayer(
                name = "series.overlay.${series.id}",
                segments = listOf(points),
                color = style.overlayLineColor,
                width = style.overlayLineWidth,
                dash = style.overlayLineDashPattern,
                join = StrokeJoin.Round,
            ),
        )
    }
    layout.refLines.forEachIndexed { index, refLine ->
        layers.add(refLineLayer(refLine, index, style, plot, density))
    }
    layout.axisTicks.filter { it.ticks.isNotEmpty() }.forEach { ticksLayout ->
        layers.add(axisLabelsLayer(ticksLayout, style, plot, formatter, density))
    }
    return layers
}

/**
 * iOS `Dictionary(uniquingKeysWith: first)` — 첫 시리즈 우선 속성 매핑. 코어가 시리즈 id 유일성을
 * 강제하지 않으므로 그리기·터치마커가 **같은 규칙**을 공유해야 중복 id 시리즈의 점이 라인과 다른
 * 축에 붙지 않는다(단일 구현 유지).
 */
internal inline fun <T> firstWinsBy(
    data: LineChartData,
    selector: (com.lumipol.graph.model.Series) -> T,
): Map<String, T> {
    val map = LinkedHashMap<String, T>()
    for (series in data.series) map.putIfAbsent(series.id, selector(series))
    return map
}

private fun firstWinsAxis(data: LineChartData): Map<String, Axis> = firstWinsBy(data) { it.axis }

private fun lineColor(axis: Axis, style: ChartStyle): Color =
    if (axis == Axis.SECONDARY) style.secondaryLineColor else style.primaryLineColor

/** 2점 미만이면 null(iOS `polylinePath` 규칙). */
private fun linePoints(points: List<NormalizedPoint>, axis: Axis, plot: PlotArea): List<PlotPoint>? {
    if (points.size < 2) return null
    return points.map { plot.point(it, axis) }
}

/** 오버레이는 자체 정규화값 — 호스트 축 반전 무시(값 클수록 위). */
private fun overlayLinePoints(points: List<NormalizedPoint>, plot: PlotArea): List<PlotPoint>? {
    if (points.size < 2) return null
    return points.map { plot.pointIgnoringInversion(it) }
}

/** X tick 세로선 + Y tick 가로선(가로선은 primary 축, 없으면 secondary). 선이 없으면 null. */
private fun gridLayer(
    layout: LineChartLayout,
    color: Color,
    style: ChartStyle,
    plot: PlotArea,
): StrokeLayer? {
    fun ticks(axis: ChartAxis) = layout.ticksFor(axis) ?: emptyList()

    val segments = mutableListOf<List<PlotPoint>>()
    for (tick in ticks(ChartAxis.X)) {
        val x = plot.x(tick.position)
        segments.add(listOf(PlotPoint(x, plot.minY), PlotPoint(x, plot.maxY)))
    }
    val primaryTicks = ticks(ChartAxis.Y_PRIMARY)
    val yTicks: List<com.lumipol.graph.model.AxisTick>
    val yAxis: Axis
    if (primaryTicks.isEmpty()) {
        yTicks = ticks(ChartAxis.Y_SECONDARY)
        yAxis = Axis.SECONDARY
    } else {
        yTicks = primaryTicks
        yAxis = Axis.PRIMARY
    }
    for (tick in yTicks) {
        val y = plot.y(tick.position, yAxis)
        segments.add(listOf(PlotPoint(plot.minX, y), PlotPoint(plot.maxX, y)))
    }
    if (segments.isEmpty()) return null
    return StrokeLayer(
        name = "grid",
        segments = segments,
        color = color,
        width = style.gridLineWidth,
        dash = style.gridLineDashPattern,
    )
}

private fun markerLayer(
    marker: com.lumipol.graph.model.MarkerLayout,
    index: Int,
    style: ChartStyle,
    plot: PlotArea,
    density: Float,
): ContainerLayer {
    val x = plot.x(marker.position)
    val children = mutableListOf<LineChartLayer>()
    children.add(
        StrokeLayer(
            name = "marker.$index.line",
            segments = listOf(listOf(PlotPoint(x, plot.minY), PlotPoint(x, plot.maxY))),
            color = if (marker.emphasis) style.markerEmphasisLineColor else style.markerLineColor,
            width = (if (marker.emphasis) MARKER_EMPHASIS_WIDTH else MARKER_WIDTH) * density,
        ),
    )
    marker.label?.let { label ->
        children.add(
            TextLayer(
                name = "marker.$index.label",
                text = label,
                anchorX = x,
                anchorY = plot.minY - LABEL_GAP * density,
                hAlign = HAlign.CENTER,
                vAlign = VAlign.ABOVE,
                color = style.axisLabelColor,
                fontSizeSp = style.axisLabelFontSize,
            ),
        )
    }
    return ContainerLayer("marker.$index", children)
}

private fun gradientLayer(
    seriesId: String,
    linePoints: List<PlotPoint>,
    axis: Axis,
    style: ChartStyle,
    plot: PlotArea,
): GradientLayer {
    // 라인 아래(플롯 바닥까지)를 닫은 area 폴리곤.
    val polygon = buildList {
        addAll(linePoints)
        add(PlotPoint(linePoints.last().x, plot.maxY))
        add(PlotPoint(linePoints.first().x, plot.maxY))
    }
    val color = lineColor(axis, style)
    return GradientLayer(
        name = "series.gradient.$seriesId",
        polygon = polygon,
        topColor = color.copy(alpha = style.gradientMaxAlpha),
        bottomColor = color.copy(alpha = 0f),
        topY = plot.minY,
        bottomY = plot.maxY,
    )
}

private fun refLineLayer(
    refLine: com.lumipol.graph.model.RefLineLayout,
    index: Int,
    style: ChartStyle,
    plot: PlotArea,
    density: Float,
): ContainerLayer {
    val y = plot.y(refLine.position, refLine.axis)
    val children = mutableListOf<LineChartLayer>()
    children.add(
        StrokeLayer(
            name = "refLine.$index.line",
            segments = listOf(listOf(PlotPoint(plot.minX, y), PlotPoint(plot.maxX, y))),
            color = style.refLineColor,
            width = REF_LINE_WIDTH * density,
            dash = style.refLineDashPattern,
        ),
    )
    refLine.label?.let { label ->
        children.add(
            TextLayer(
                name = "refLine.$index.label",
                text = label,
                anchorX = plot.maxX,
                anchorY = y - LABEL_GAP * density,
                hAlign = HAlign.RIGHT,
                vAlign = VAlign.ABOVE,
                color = style.refLineColor, // iOS: 기준선 라벨은 refLineColor
                fontSizeSp = style.axisLabelFontSize,
            ),
        )
    }
    return ContainerLayer("refLine.$index", children)
}

private fun axisLabelsLayer(
    ticksLayout: com.lumipol.graph.model.AxisTicksLayout,
    style: ChartStyle,
    plot: PlotArea,
    formatter: (ChartAxis, Double) -> String,
    density: Float,
): ContainerLayer {
    val children = ticksLayout.ticks.mapIndexed { i, tick ->
        val text = formatter(ticksLayout.axis, tick.value)
        when (ticksLayout.axis) {
            ChartAxis.X -> TextLayer(
                name = "axisLabels.x.$i",
                text = text,
                anchorX = plot.x(tick.position),
                anchorY = plot.maxY + AXIS_LABEL_GAP * density,
                hAlign = HAlign.CENTER,
                vAlign = VAlign.BELOW,
                color = style.axisLabelColor,
                fontSizeSp = style.axisLabelFontSize,
            )
            ChartAxis.Y_PRIMARY -> TextLayer(
                name = "axisLabels.yPrimary.$i",
                text = text,
                anchorX = plot.minX - AXIS_LABEL_GAP * density,
                anchorY = plot.y(tick.position, Axis.PRIMARY),
                hAlign = HAlign.RIGHT,
                vAlign = VAlign.CENTER,
                color = style.axisLabelColor,
                fontSizeSp = style.axisLabelFontSize,
            )
            else -> TextLayer( // Y_SECONDARY (및 방어적 기본)
                name = "axisLabels.ySecondary.$i",
                text = text,
                anchorX = plot.maxX + AXIS_LABEL_GAP * density,
                anchorY = plot.y(tick.position, Axis.SECONDARY),
                hAlign = HAlign.LEFT,
                vAlign = VAlign.CENTER,
                color = style.axisLabelColor,
                fontSizeSp = style.axisLabelFontSize,
            )
        }
    }
    return ContainerLayer("axisLabels.${axisName(ticksLayout.axis)}", children)
}

private fun axisName(axis: ChartAxis): String = when (axis) {
    ChartAxis.X -> "x"
    ChartAxis.Y_PRIMARY -> "yPrimary"
    else -> "ySecondary"
}

// =====================================================================================
// DrawScope 렌더 — 순수 모델 → 픽셀. z-순서: 배경 area → 1~8(clipRect) → 축라벨(clip 밖).
// =====================================================================================

/**
 * [buildLineChartLayers] 결과를 그린다. arch 결정: 시리즈/그리드/밴드/마커/기준선(1~8)은
 * `clipRect(plot)` 안에서, **축 라벨은 clip 밖**(플롯 여백에 라벨이 보이게). 배경 area 실루엣은
 * 최하단(그리드 아래). 터치 마커는 상위(배치3 [drawTouchMarker])가 별도로 그린다.
 *
 * ### 클립 top을 뷰 상단(0f)까지 여는 이유 (QA Major-1 대응)
 * iOS([RDChartView.updateClipMask])는 클립을 두 경우로 나눈다: **비확대 시 무클립**(`mask=nil`),
 * **확대 시** 마스크를 `CGRect(x: plot.minX, y: 0, width: plot.width, height: plot.maxY)`로 —
 * 즉 좌우는 플롯 경계로 닫되 **top은 뷰 상단(y=0)까지 열어** 플롯 상단 여백에 그리는 구간(km) 마커
 * 라벨([markerLayer]의 `anchorY = plot.minY - gap`)이 잘리지 않게 한다("마스크 위쪽을 뷰 상단까지
 * 열어 구간 마커 라벨은 잘리지 않게 한다" — iOS 주석). 여기서 `top=0f` 단일 클립은 iOS 확대 마스크와
 * 기하가 정확히 동치이고, 비확대에서도 모든 콘텐츠가 이미 `[plot.minX, plot.maxX] × (…, plot.maxY]`
 * 안에 들어오므로(경계 밖 유일 요소인 상단 라벨은 top=0으로 보존됨) 무클립과 시각적으로 동등하다.
 * (이전 `top = plot.minY`는 상단 여백의 마커/기준선 라벨을 전부 잘라 화면에서 사라지게 했다.)
 *
 * @param entranceProgress main 시리즈 등장 애니(0~1). PathMeasure로 길이비율만큼 잘라 그린다.
 */
internal fun DrawScope.drawLineChart(
    layout: LineChartLayout,
    data: LineChartData,
    style: ChartStyle,
    plot: PlotArea,
    formatter: (ChartAxis, Double) -> String,
    measurer: TextMeasurer,
    background: AreaFillLayer? = null,
    entranceProgress: Float = 1f,
) {
    if (!plot.isRenderable) return
    // 마커/기준선 폭·라벨 여백 등 스타일 밖 상수도 dp→px 환산(DrawScope.density; style은 이미 스케일됨).
    val layers = buildLineChartLayers(layout, data, style, plot, formatter, density)
    val (axisLabels, clipped) = layers.partition { it.name.startsWith("axisLabels.") }
    drawPartitionedLayers(plot, background, clipped, axisLabels, measurer, entranceProgress, cache = null)
}

/**
 * [drawLineChart]의 캐시 경로 — 스크럽 프레임(마커만 변함)마다 레이어·실루엣·Path 재계산을 피한다.
 * 입력이 바뀌면 [LineChartDrawCache.update]가 재조립하고, 같으면 이전 인스턴스를 그대로 그린다.
 */
internal fun DrawScope.drawCachedLineChart(
    cache: LineChartDrawCache,
    layout: LineChartLayout,
    data: LineChartData,
    style: ChartStyle,
    plot: PlotArea,
    formatter: (ChartAxis, Double) -> String,
    measurer: TextMeasurer,
    sortedArea: List<com.lumipol.graph.model.Point>? = null,
    entranceProgress: Float = 1f,
) {
    if (!plot.isRenderable) return
    cache.update(layout, data, style, plot, formatter, density, sortedArea)
    drawPartitionedLayers(plot, cache.background, cache.clipped, cache.axisLabels, measurer, entranceProgress, cache)
}

private fun DrawScope.drawPartitionedLayers(
    plot: PlotArea,
    background: AreaFillLayer?,
    clipped: List<LineChartLayer>,
    axisLabels: List<LineChartLayer>,
    measurer: TextMeasurer,
    entranceProgress: Float,
    cache: LineChartDrawCache?,
) {
    clipRect(
        left = plot.minX.toFloat(),
        top = 0f, // 뷰 상단까지 열어 상단 여백의 마커/기준선 라벨 보존 (iOS 확대 마스크 y=0과 동치)
        right = plot.maxX.toFloat(),
        bottom = plot.maxY.toFloat(),
    ) {
        background?.let { render(it, measurer, entranceProgress, cache) }
        clipped.forEach { render(it, measurer, entranceProgress, cache) }
    }
    axisLabels.forEach { render(it, measurer, entranceProgress, cache) }
}

/**
 * 정적 그리기 산출물 캐시 — 스크럽 중 draw는 60~120Hz로 무효화되지만 마커 외 콘텐츠는 그대로다.
 * (layout, data, style, plot, formatter, density, area)가 모두 같으면 레이어 리스트·배경 실루엣·
 * Path 인스턴스를 재사용한다. RDLineChart가 interaction 수명으로 remember한다.
 */
internal class LineChartDrawCache {
    private var key: List<Any?>? = null

    /** clip 안에 그리는 레이어(시리즈/그리드/밴드/마커/기준선). */
    var clipped: List<LineChartLayer> = emptyList()
        private set

    /** clip 밖(플롯 여백)에 그리는 축 라벨. */
    var axisLabels: List<LineChartLayer> = emptyList()
        private set

    /** 배경 area 실루엣(최하단). sortedArea가 없거나 X 스케일 불능이면 null. */
    var background: AreaFillLayer? = null
        private set

    private val paths = HashMap<LineChartLayer, Path>()

    /** 입력이 바뀌었을 때만 재조립. @return 재조립 여부. */
    fun update(
        layout: LineChartLayout,
        data: LineChartData,
        style: ChartStyle,
        plot: PlotArea,
        formatter: (ChartAxis, Double) -> String,
        density: Float,
        sortedArea: List<com.lumipol.graph.model.Point>?,
    ): Boolean {
        val newKey = listOf(
            layout, data, style,
            plot.minX, plot.minY, plot.width, plot.height, plot.invertedAxes,
            formatter, density, sortedArea,
        )
        if (newKey == key) return false
        key = newKey
        paths.clear()
        val layers = buildLineChartLayers(layout, data, style, plot, formatter, density)
        val split = layers.partition { it.name.startsWith("axisLabels.") }
        axisLabels = split.first
        clipped = split.second
        background = sortedArea?.let { area ->
            layout.ticksFor(ChartAxis.X)
                ?.let { AxisScale.from(it) }
                ?.let { xScale -> AreaSilhouette.build(area, xScale, plot, style) }
        }
        return true
    }

    /** 같은 레이어(값 동등)는 같은 Path 인스턴스 — 프레임마다 moveTo/lineTo 재생성 방지. */
    fun strokePath(layer: StrokeLayer): Path = paths.getOrPut(layer) { polylinePath(layer.segments) }

    fun polygonPathOf(layer: LineChartLayer, polygon: List<PlotPoint>): Path =
        paths.getOrPut(layer) { polygonPath(polygon) }
}

/** 단일 레이어 렌더(재귀). 배치2·3 공용(터치 마커·바·도넛도 이 디스패치 재사용).
 *  [cache]가 있으면 Path를 재사용한다(스크럽 프레임 재생성 방지). */
internal fun DrawScope.render(
    layer: LineChartLayer,
    measurer: TextMeasurer,
    entranceProgress: Float = 1f,
    cache: LineChartDrawCache? = null,
) {
    when (layer) {
        is StrokeLayer -> {
            val stroke = Stroke(
                width = layer.width,
                cap = layer.cap,
                join = layer.join,
                pathEffect = layer.dash?.let { PathEffect.dashPathEffect(it, 0f) },
            )
            val path = cache?.strokePath(layer) ?: polylinePath(layer.segments)
            val drawn = if (layer.trimmable && entranceProgress < 1f) trimPath(path, entranceProgress) else path
            drawPath(drawn, color = layer.color, style = stroke)
        }
        is RectLayer -> {
            val topLeft = Offset(layer.minX.toFloat(), layer.minY.toFloat())
            val size = Size(layer.width.toFloat(), layer.height.toFloat())
            // iOS `barLayer.cornerRadius = style.barCornerRadius`(3dp) 재현. 밴드는 cornerRadius=0이라 직각.
            if (layer.cornerRadius > 0f) {
                drawRoundRect(
                    color = layer.color,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(layer.cornerRadius, layer.cornerRadius),
                    alpha = layer.alpha,
                )
            } else {
                drawRect(color = layer.color, topLeft = topLeft, size = size, alpha = layer.alpha)
            }
        }
        is DotLayer -> drawCircle(
            color = layer.color,
            radius = layer.radius,
            center = Offset(layer.center.x.toFloat(), layer.center.y.toFloat()),
        )
        is GradientLayer -> {
            val path = cache?.polygonPathOf(layer, layer.polygon) ?: polygonPath(layer.polygon)
            drawPath(
                path,
                brush = Brush.verticalGradient(
                    colors = listOf(layer.topColor, layer.bottomColor),
                    startY = layer.topY.toFloat(),
                    endY = layer.bottomY.toFloat(),
                ),
                style = Fill,
            )
        }
        is AreaFillLayer -> drawPath(
            cache?.polygonPathOf(layer, layer.polygon) ?: polygonPath(layer.polygon),
            color = layer.color,
            style = Fill,
        )
        is TextLayer -> drawAlignedText(measurer, layer)
        is ContainerLayer -> layer.children.forEach { render(it, measurer, entranceProgress, cache) }
    }
}

private fun polylinePath(segments: List<List<PlotPoint>>): Path {
    val path = Path()
    for (seg in segments) {
        if (seg.isEmpty()) continue
        path.moveTo(seg[0].x.toFloat(), seg[0].y.toFloat())
        for (i in 1 until seg.size) path.lineTo(seg[i].x.toFloat(), seg[i].y.toFloat())
    }
    return path
}

private fun polygonPath(points: List<PlotPoint>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x.toFloat(), points[0].y.toFloat())
    for (i in 1 until points.size) path.lineTo(points[i].x.toFloat(), points[i].y.toFloat())
    path.close()
    return path
}

private fun trimPath(path: Path, progress: Float): Path {
    val measure = PathMeasure()
    measure.setPath(path, false)
    val dst = Path()
    measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), dst, true)
    return dst
}

/** 측정된 폭·높이로 앵커+정렬을 픽셀 원점으로 환산해 그린다(iOS `addLabel`/`textLayer` 정렬 재현). */
internal fun DrawScope.drawAlignedText(measurer: TextMeasurer, label: TextLayer) {
    // 접근성 글꼴 배율 상한(UX Minor-1): 사용자가 시스템 글꼴을 크게 키워도 축/마커 라벨이 여백을 넘어
    // 겹치거나 잘리지 않도록 배율을 1.3까지만 반영한다. sp는 density+fontScale로 스케일되므로, fontScale이
    // 상한을 넘으면 상한/현재배율 비율로 sp를 줄여 유효 배율을 고정한다.
    val effectiveSp = if (fontScale > MAX_FONT_SCALE) {
        label.fontSizeSp * (MAX_FONT_SCALE / fontScale)
    } else {
        label.fontSizeSp
    }
    // tnum: 숫자 폭 고정 → 줌으로 tick 값이 바뀔 때 라벨이 좌우로 떨리지 않음(UX Minor-2).
    val result = measurer.measure(
        label.text,
        style = TextStyle(fontSize = effectiveSp.sp, fontFeatureSettings = "tnum"),
    )
    val w = result.size.width.toFloat()
    val h = result.size.height.toFloat()
    val originX = when (label.hAlign) {
        HAlign.LEFT -> label.anchorX.toFloat()
        HAlign.CENTER -> label.anchorX.toFloat() - w / 2f
        HAlign.RIGHT -> label.anchorX.toFloat() - w
    }
    val originY = when (label.vAlign) {
        VAlign.ABOVE -> label.anchorY.toFloat() - h
        VAlign.BELOW -> label.anchorY.toFloat()
        VAlign.CENTER -> label.anchorY.toFloat() - h / 2f
    }
    drawText(result, color = label.color, topLeft = Offset(originX, originY))
}

// 매직 넘버(iOS 원본 상수) — 라벨 여백·선 폭.
private const val LABEL_GAP = 2.0            // 마커/기준선 라벨과 선 사이 여백(iOS -2)
private const val AXIS_LABEL_GAP = 4.0       // 축 라벨과 플롯 경계 여백(iOS ±4)
private const val MARKER_WIDTH = 1f
private const val MARKER_EMPHASIS_WIDTH = 1.5f
private const val REF_LINE_WIDTH = 1f
/** 접근성 글꼴 배율 상한(UX Minor-1) — 라벨이 플롯 여백을 넘지 않게 1.3까지만 키운다. */
private const val MAX_FONT_SCALE = 1.3f
