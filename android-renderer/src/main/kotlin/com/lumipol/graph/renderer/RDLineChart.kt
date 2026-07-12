// iOS: RDChartView.swift (render 파이프라인·그리기·상태 배선 — UIView → @Composable)
//
// 파이프라인: LineChartEngine.layout → PlotArea(픽셀) → drawLineChart(레이어) → drawTouchMarker.
// 명령형 render()/setNeedsLayout()은 선언형으로 대체: data/style은 불변 파라미터, layout은
// derivedStateOf 파생(줌 창→재계산), 상호작용 상태는 [LineChartInteraction] 홀더가 소유한다.
package com.lumipol.graph.renderer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.Point
import java.util.Locale

/** 라인 등장 애니 지속시간(ms) — Material Emphasized(iOS strokeEnd 0.6s). */
private const val ENTRANCE_DURATION_MS = 600

/**
 * 기본 축 라벨 포매터(iOS `defaultFormatter` = `String(format: "%g", value)`와 동일 출력).
 * 유효숫자 6자리, 트레일링 0 제거, C `%g` 규칙의 소수/지수 표기 전환(지수 < -4 또는 ≥ 6이면 지수 표기 —
 * 예: 1234567.89 → "1.23457e+06"). Kotlin/Java `%g`는 트레일링 0을 유지하므로 포맷 후 직접 정리한다.
 * 앱이 단위 포매터를 주입하지 않으면 사용.
 */
fun defaultLineChartFormatter(axis: ChartAxis, value: Double): String {
    // C %g의 nan/inf 표기(iOS 동일) — Java는 "NaN"/"Infinity"라 별도 처리.
    if (value.isNaN()) return "nan"
    if (value.isInfinite()) return if (value > 0) "inf" else "-inf"
    // Locale 고정 — 소수점 문자가 기기 지역 설정을 타지 않는다(iOS %g도 C 로케일 표기).
    val formatted = String.format(Locale.ROOT, "%g", value)
    val exponent = formatted.indexOf('e')
    return if (exponent >= 0) {
        trimTrailingZeros(formatted.substring(0, exponent)) + formatted.substring(exponent)
    } else {
        trimTrailingZeros(formatted)
    }
}

/** `"5.00000"` → `"5"`, `"1.230000"` → `"1.23"`. 소수점이 없으면 그대로(C %g의 0 제거 규칙). */
private fun trimTrailingZeros(text: String): String =
    if ('.' in text) text.trimEnd('0').trimEnd('.') else text

/**
 * 페이스/심박 라인차트. 스크럽(터치 마커+콜백)·핀치 줌·확대 팬·배경 area·등장 애니를 지원한다.
 *
 * @param data 코어 [LineChartData]. 시리즈 없이 [backgroundArea]만 있으면 area 단독 모드로 폴백한다.
 * @param invertedAxes 화면에서 뒤집을 Y축(예: 페이스 — 위=빠름). 코어 출력은 값-공간 그대로.
 * @param backgroundArea 페이스 라인 뒤 배경 고도 실루엣(장식). 스크럽 시 보간 실값을 [onScrubBackground]로.
 * @param labelFormatter 축 tick 값 → 표시 문자열. 오버레이 시리즈가 있으면 [ChartAxis.Y_OVERLAY]로도 호출.
 * @param isZoomEnabled X축 핀치 줌 + 확대 팬 활성화(기본 off).
 * @param maxZoomScale 최대 확대 배율(전체 구간 대비).
 * @param zoomXRange 프로그래매틱 줌 창(원본 X 도메인). iOS `zoom(toXRange:)` 대응 — non-null이고
 *   [isZoomEnabled]일 때 값이 바뀌면 해당 구간으로 확대한다(제스처 줌과 병행; null은 "요청 없음"이라
 *   자동 리셋하지 않음 — 전체 복귀는 [isZoomEnabled]=false 또는 더블탭/핀치 아웃 경로).
 *   선언형 경로라 **같은 값 재지정은 무시**된다 — 같은 구간 재요청이 필요하면 [zoomController]를 사용.
 * @param zoomController 명령형 줌 요청 핸들([LineChartZoomController.zoomTo]/
 *   [LineChartZoomController.reset]) — 같은 구간 재요청도 매번 적용된다(iOS `zoom(toXRange:)`/
 *   `resetZoom` 재호출 parity).
 * @param markerController 명령형 터치 마커 핸들([LineChartMarkerController.show]/
 *   [LineChartMarkerController.hide]) — iOS `showTouchMarker(atX:)`/`hideTouchMarker()` 대응.
 *   show는 제스처 스크럽과 동일 경로로 콜백을 발화한다(패리티 규칙은 [LineChartMarkerController] 참조).
 * @param animateEntrance main 라인 등장 애니(첫 layout 1회). 스냅샷/테스트에선 false.
 * @param onScrub 스크럽 근접점 값(seriesId→포맷문자열). @param onScrubBackground 배경 area 보간 실값.
 * @param onScrubEnd 스크럽 종료(마커가 표시 중이었을 때만). 짝맞춤 불변식은 [LineChartInteraction] 참조.
 */
@Composable
fun RDLineChart(
    data: LineChartData,
    modifier: Modifier = Modifier,
    style: ChartStyle = ChartStyle.defaults(isSystemInDarkTheme()),
    invertedAxes: Set<Axis> = emptySet(),
    backgroundArea: List<Point>? = null,
    labelFormatter: (ChartAxis, Double) -> String = ::defaultLineChartFormatter,
    isZoomEnabled: Boolean = false,
    maxZoomScale: Float = 10f,
    zoomXRange: ClosedRange<Double>? = null,
    zoomController: LineChartZoomController? = null,
    markerController: LineChartMarkerController? = null,
    animateEntrance: Boolean = true,
    onScrub: OnScrub? = null,
    onScrubBackground: OnScrubBackground? = null,
    onScrubEnd: OnScrubEnd? = null,
) {
    // 코어 interpolatedY 이진탐색은 x 오름차순 전제 — 저장 시 정규화(실루엣 렌더는 순서 무관).
    val sortedArea = remember(backgroundArea) { backgroundArea?.sortedBy { it.x } }
    // 상호작용 홀더 — data/area가 바뀌면 새 인스턴스(줌·마커 리셋 = iOS render() 초기화).
    val interaction = remember(data, sortedArea) { LineChartInteraction(data, sortedArea) }

    // 최신 콜백·설정을 홀더에 주입(재구성 시 stale 람다 방지).
    SideEffect {
        interaction.onScrub = onScrub
        interaction.onScrubBackground = onScrubBackground
        interaction.onScrubEnd = onScrubEnd
        interaction.maxZoomScale = maxZoomScale.toDouble()
    }
    // formatter는 pointerInput 키 밖이라 stale 위험(Arch m2). 최신 람다를 State로 잡아 제스처가 항상
    // 현재 formatter로 스크럽 값을 포맷하게 한다(콜백은 위 SideEffect로 이미 최신).
    val currentFormatter by rememberUpdatedState(labelFormatter)

    // dp 의미의 스타일 기하 값을 px로 환산(실기기 렌더 붕괴 방지 — UX Critical-1). 그리기·히트테스트가
    // 같은 스케일을 써야 하므로 한 번 만들어 양쪽(제스처 plot·draw plot)에 공유한다.
    val density = LocalDensity.current.density
    val scaledStyle = remember(style, density) { style.scaledForDensity(density) }
    // 줌 비활성화 시 전체 구간 복귀(iOS isZoomEnabled didSet → resetZoom).
    LaunchedEffect(interaction, isZoomEnabled) {
        if (!isZoomEnabled) interaction.resetZoom()
    }
    // 프로그래매틱 줌(iOS zoom(toXRange:)). 활성 + non-null일 때 요청 구간으로 확대.
    LaunchedEffect(interaction, isZoomEnabled, zoomXRange) {
        if (isZoomEnabled) zoomXRange?.let { interaction.zoomToRange(it.start..it.endInclusive) }
    }
    // 명령형 줌 요청 — 같은 구간 재요청도 매번 적용(iOS zoom(toXRange:) 재호출 parity).
    // range == null은 명시 리셋(iOS resetZoom) — 전체 구간 zoomTo의 클램프→null 정규화와 같은 상태.
    // 키에 interaction을 넣지 않는다: 데이터 갱신으로 interaction이 재생성될 때 스테일 요청 봉투가
    // 재적용되면 안 된다 — iOS render()는 zoomState를 초기화하고 이전 zoom(toXRange:)을 기억하지 않는다
    // (선언형 zoomXRange는 "원하는 상태"라서 예외 — 위 이펙트는 데이터 갱신 후에도 재적용 유지).
    val zoomRequest = zoomController?.request
    LaunchedEffect(isZoomEnabled, zoomRequest) {
        if (isZoomEnabled) {
            zoomRequest?.let { req ->
                val range = req.range
                if (range != null) interaction.zoomToRange(range) else interaction.resetZoom()
            }
        }
    }

    // 줌 창 → layout 파생(iOS commitViewport/makeFullLayout). zoom은 관측 state라 창 변경 시 재계산.
    val layout by remember(interaction) { derivedStateOf { interaction.layoutForCurrentWindow() } }

    val measurer = rememberTextMeasurer()
    val haptics = LocalHapticFeedback.current
    // 등장 애니 — 컴포저블 수명당 1회. interaction을 키로 걸면 data 갱신(스트리밍)마다 재생성돼
    // 라인이 매번 0%부터 다시 그려진다. 줌 relayout·데이터 갱신 모두 재실행 금지.
    val progress = remember { Animatable(if (animateEntrance) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateEntrance) progress.animateTo(1f, tween(ENTRANCE_DURATION_MS, easing = EmphasizedDecelerate))
    }

    // TalkBack 요약(UX Major-1): 캔버스는 불투명하므로 컨테이너에 시리즈 구성을 노출한다.
    val description = remember(data, sortedArea) { lineChartDescription(data, sortedArea) }

    // 정적 그리기 캐시 — interaction 수명(데이터 교체 시 새로 생성돼 이전 Path 해제).
    val drawCache = remember(interaction) { LineChartDrawCache() }

    // 플롯 좌표계는 제스처와 그리기가 반드시 동일해야 한다(스크럽 드리프트 방지) — 단일 생성자.
    val buildPlot = remember(scaledStyle, invertedAxes) {
        { w: Double, h: Double ->
            if (w <= 0.0 || h <= 0.0) {
                null
            } else {
                PlotArea(w, h, scaledStyle.plotInsets, invertedAxes).takeIf { it.isRenderable }
            }
        }
    }

    // 명령형 마커 요청(iOS showTouchMarker/hideTouchMarker). 마커 조립엔 플롯(캔버스 크기)이 필요하므로
    // onSizeChanged로 크기를 추적하고, 배치 전(크기 0) 요청은 조용히 무시한다(iOS currentPlotArea==nil 가드).
    // 같은 x 재요청도 봉투 항등성으로 매번 발화한다. 표시 후 relayout 복원은 activeMarkerRawX 파생이 담당.
    // 키는 markerRequest만: 데이터 갱신으로 interaction이 재생성돼도 스테일 show 요청이 재적용되어
    // onScrub이 재발화되면 안 된다 — iOS render()는 마커를 **무통지** 제거하고 이전 show를 기억하지 않는다.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val markerRequest = markerController?.request
    LaunchedEffect(markerRequest) {
        val request = markerRequest ?: return@LaunchedEffect
        val rawX = request.rawX
        if (rawX == null) {
            interaction.endScrub()
        } else {
            buildPlot(canvasSize.width.toDouble(), canvasSize.height.toDouble())?.let { plot ->
                val ctx = TouchMarkerContext(
                    data, interaction.layoutForCurrentWindow(), scaledStyle, plot,
                    { axis, value -> currentFormatter(axis, value) }, density,
                    axisBySeriesId = interaction.axisBySeriesId,
                    roleBySeriesId = interaction.roleBySeriesId,
                )
                interaction.scrubTo(rawX, ctx, notify = true)
            }
        }
    }

    Canvas(
        modifier
            .onSizeChanged { canvasSize = it }
            .semantics { contentDescription = description }
            .pointerInput(interaction, isZoomEnabled, scaledStyle) {
                lineChartGestures(
                    interaction = interaction,
                    data = data,
                    style = scaledStyle,
                    isZoomEnabled = isZoomEnabled,
                    formatter = { axis, value -> currentFormatter(axis, value) },
                    haptics = haptics,
                    layoutProvider = { interaction.layoutForCurrentWindow() },
                    plotProvider = { buildPlot(size.width.toDouble(), size.height.toDouble()) },
                )
            },
    ) {
        val plot = buildPlot(size.width.toDouble(), size.height.toDouble()) ?: return@Canvas
        val currentLayout = layout

        // 정적 콘텐츠(레이어·실루엣·Path)는 캐시 — 스크럽은 draw만 60~120Hz로 무효화하므로
        // 마커 외 재계산을 입력 변경 시로 한정한다(스크럽 프레임 예산 확보).
        drawCachedLineChart(
            cache = drawCache,
            layout = currentLayout,
            data = data,
            style = scaledStyle,
            plot = plot,
            formatter = currentFormatter,
            measurer = measurer,
            sortedArea = sortedArea,
            entranceProgress = progress.value,
            // 확대 상태에서만 플롯 클립(iOS updateClipMask parity) — 1x는 무클립으로 가장자리 캡 보존.
            isZoomed = interaction.zoom?.isZoomed == true,
        )

        // 터치 마커는 활성 rawX에서 매 프레임 재파생 → relayout(회전/리사이즈) 생존, 콜백 무발화.
        interaction.activeMarkerRawX?.let { rawX ->
            val ctx = TouchMarkerContext(
                data, currentLayout, scaledStyle, plot, currentFormatter, density,
                axisBySeriesId = interaction.axisBySeriesId,
                roleBySeriesId = interaction.roleBySeriesId,
            )
            val result = if (data.series.isEmpty()) {
                TouchMarker.makeBackgroundOnly(rawX, ctx)
            } else {
                TouchMarker.make(rawX, ctx)
            }
            result?.let { drawTouchMarker(it, measurer) }
        }
    }
}

/** 라인 차트 TalkBack 요약(UX Major-1). 시리즈 수·배경 area 유무를 낭독. */
private fun lineChartDescription(data: LineChartData, sortedArea: List<Point>?): String {
    val seriesCount = data.series.size
    return when {
        seriesCount == 0 && sortedArea.isNullOrEmpty() -> "라인 차트, 데이터 없음"
        seriesCount == 0 -> "라인 차트, 배경 고도 영역"
        else -> "라인 차트, 시리즈 ${seriesCount}개" + if (!sortedArea.isNullOrEmpty()) ", 배경 고도 영역 포함" else ""
    }
}
