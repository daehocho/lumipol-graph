// iOS: ChartStyle.swift
package com.lumipol.graph.renderer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.lumipol.graph.ChartDefaults
import com.lumipol.graph.model.DonutColorRole

/**
 * 플롯 내부 여백 (좌우 = Y축 라벨, 상하 = 마커/X축 라벨).
 * iOS `UIEdgeInsets` 대응. 값은 dp 단위(Compose 밀도 변환은 DrawScope 경계에서 수행).
 */
@Immutable
data class Insets(
    val top: Float,
    val left: Float,
    val bottom: Float,
    val right: Float,
)

/**
 * 차트 팔레트·타이포·여백. 앱이 통째로 주입해 커스터마이징한다.
 *
 * iOS `ChartStyle`(동적 `UIColor` 기본값)의 Android 대응. Compose엔 자동 동적 색이 없으므로
 * 라이트/다크 두 세트를 [defaults]로 명시 제공한다(`isSystemInDarkTheme()` 판정은 호출부 책임).
 * **브랜드 데이터 색(시리즈·존·바 역할)은 Apple 시스템색 실측 RGB를 라이트/다크 쌍으로 고정**하여
 * 앱 테마와 무관하게 "존5=빨강, 페이스=파랑" 같은 의미 색이 일정하게 보이도록 한다(MaterialTheme 미의존).
 *
 * dash 패턴은 iOS `[NSNumber]`(px) → [FloatArray](px). 색 alpha는 iOS `withAlphaComponent` 대응으로
 * [Color.copy]를 사용한다. 숫자 값(라인 폭·여백·비율·dash)은 iOS와 정확히 일치한다.
 */
@Immutable
data class ChartStyle(
    // 시리즈 라인
    val lineWidth: Float = ChartDefaults.LINE_WIDTH.toFloat(),
    val primaryLineColor: Color,
    val secondaryLineColor: Color,
    /**
     * 선택된 **모든** 시리즈(축·역할 무관, overlay 포함)에 각자 색의 area 그라데이션을 그릴 때
     * 시작 알파. 0이면 그라데이션 없음. 여러 장이 겹칠 때 탁해지지 않도록 실제 시작 알파는
     * `gradientMaxAlpha / √n`(n=그라데이션 장수)으로 감쇠한다 — n=1이면 이 값 그대로.
     *
     * 감쇠의 한계: n은 실제 겹침이 아니라 **그리기 가능한 전체 시리즈 수**다. x 범위가 겹치지
     * 않는 시리즈도 서로를 감쇠시키고(단독 구간이 α/√n로 흐려짐), 겹치는 구간의 합성 불투명도는
     * `1-(1-α/√n)^n`로 n과 함께 서서히 는다(α=0.25: n=2→0.33, n=9→0.54). 선택 상한이 없으므로
     * 동시 선택이 많은 화면은 이 값을 낮추거나 0으로 끄는 편이 안전하다.
     */
    val gradientMaxAlpha: Float = ChartDefaults.GRADIENT_MAX_ALPHA.toFloat(),
    /**
     * 시리즈 id → 색. 지정되면 라인·배경 그라데이션이 이 색을 쓴다(축 슬롯 색보다 우선).
     * 비어 있으면 종전대로 축/역할 기반 색으로 폴백. 클로저가 아니라 Map인 이유는
     * LineChartDrawCache가 style을 값 동등성으로 캐시 키에 쓰기 때문(람다면 매 프레임 미스).
     */
    val seriesColors: Map<String, Color> = emptyMap(),

    // 그리드 (X tick 세로선 + Y tick 가로선). null이면 그리드 없음.
    val gridLineColor: Color?,
    val gridLineDashPattern: FloatArray = floatArrayOf(ChartDefaults.GRID_DASH_ON.toFloat(), ChartDefaults.GRID_DASH_OFF.toFloat()),
    val gridLineWidth: Float = ChartDefaults.GRID_LINE_WIDTH.toFloat(),

    // 오버레이(코어가 자체 정규화한 시리즈) — 축 라벨 없는 가는 실선 라인(0.23.0부터 점선 제거).
    // 배경 그라데이션은 다른 시리즈와 동일하게 gradientMaxAlpha 규칙을 따른다(0.21.0부터).
    val overlayLineColor: Color,
    val overlayLineWidth: Float = ChartDefaults.OVERLAY_LINE_WIDTH.toFloat(),

    // 기준선/밴드 (refLineDashPattern은 BarChart 평균/목표 점선이 재사용)
    val refLineDashPattern: FloatArray = floatArrayOf(ChartDefaults.REF_DASH_ON.toFloat(), ChartDefaults.REF_DASH_OFF.toFloat()),
    val refBandColor: Color,

    // 배경 고도 실루엣 (장식 area — 축/스크럽 없음)
    val areaFillColor: Color,
    val areaHeightFraction: Float = ChartDefaults.AREA_HEIGHT_FRACTION.toFloat(),
    // 실루엣 높이 정규화 분모의 하한(도메인 단위 — 고도면 m). 실측 고저차가 이보다 작으면
    // 그만큼 납작하게 그려져 센서 노이즈가 산맥으로 보이지 않는다. 0이면 하한 없음.
    val areaMinValueSpan: Double = ChartDefaults.AREA_MIN_VALUE_SPAN,

    // 구간(km) 마커
    val markerLineColor: Color,
    val markerEmphasisLineColor: Color,

    // 스플릿 막대
    val barWidthRatio: Float = ChartDefaults.BAR_WIDTH_RATIO.toFloat(),   // 슬롯 폭 대비 막대 폭(iOS slot*0.6)
    val partialBarAlpha: Float = ChartDefaults.PARTIAL_BAR_ALPHA.toFloat(), // 부분 스플릿 막대 흐림
    val barCornerRadius: Float = ChartDefaults.BAR_CORNER_RADIUS.toFloat(),
    val barShowYAxisLabels: Boolean = true, // false면 y틱 라벨 숨김(그리드·참조선은 유지)
    val barShowXAxisLabels: Boolean = true, // false면 x축 하단 라벨 숨김
    val barReferenceLineColor: Color,
    val barMinHeight: Float = ChartDefaults.BAR_MIN_HEIGHT.toFloat(), // 가장 빠른(짧은) 막대도 최소 가시 높이
    val barDimOpacity: Float = ChartDefaults.BAR_DIM_OPACITY.toFloat(), // 롱프레스 선택 시 미선택 막대 흐림 배율(iOS barDimOpacity)
    /**
     * 막대별 색 오버라이드. null이면 코어 [com.lumipol.graph.PaceColormap] 사용.
     * 앱은 stable 람다를 넘길 것(리컴포지션 방지).
     * 단계적 폐기 예정(B6/C4) — 색약 모드는 [colorBlindMode] 주입으로 대체한다.
     */
    val barColorProvider: ((BarPaceColorInput) -> Color)? = null,
    /** 색약 보정 모드 — 코어 컬러맵이 이산 4색(Okabe-Ito 계열)으로 전환(B6, D12). */
    val colorBlindMode: Boolean = false,
    val barSelectionLineColor: Color, // 선택 막대 세로 가이드선(iOS label α0.55)
    val barCalloutBackgroundColor: Color, // 말풍선 배경(iOS .label)
    val barCalloutTextColor: Color, // 말풍선 텍스트(iOS .systemBackground)
    val barCalloutFontSize: Float = ChartDefaults.BAR_CALLOUT_FONT_SIZE.toFloat(), // 말풍선 폰트 크기(sp) — iOS systemFont(12, .semibold)
    val barCalloutFontWeight: FontWeight = FontWeight.SemiBold,

    // 심박존 도넛
    val donutColors: Map<DonutColorRole, Color>,
    val donutRingWidth: Float = ChartDefaults.DONUT_RING_WIDTH.toFloat(),
    val donutEmptyColor: Color,

    // 심박존 도넛 — 탭 선택(0.26.0). 색은 라이트/다크 팔레트에서 주입.
    val donutDimmedAlpha: Float = ChartDefaults.DONUT_DIMMED_ALPHA.toFloat(),                 // 비선택 세그먼트 alpha(원 alpha 대체)
    val donutCenterLabelFontSize: Float = ChartDefaults.DONUT_CENTER_LABEL_FONT_SIZE.toFloat(),          // 센터 존 이름(sp)
    val donutCenterLabelColor: Color,                   // iOS .secondaryLabel 대응
    val donutCenterPercentFontSize: Float = ChartDefaults.DONUT_CENTER_PERCENT_FONT_SIZE.toFloat(),        // 센터 퍼센트(sp)
    val donutCenterPercentFontWeight: FontWeight = FontWeight.Bold,
    val donutCenterPercentColor: Color,                 // iOS .label 대응
    val donutAutoDeselectDelaySeconds: Float = ChartDefaults.DONUT_AUTO_DESELECT_SECONDS.toFloat(),      // 0 이하면 자동 해제 없음
    val donutSelectionHapticsEnabled: Boolean = true,

    // 축 라벨 (iOS `axisLabelFont: UIFont` → 크기·패밀리·웨이트로 분해 보관, TextStyle 조립은 draw 경계.
    // 모든 라벨 TextLayer(축/마커/기준선/바)가 공유한다 — iOS도 전부 axisLabelFont 단일 폰트.)
    val axisLabelFontSize: Float = ChartDefaults.AXIS_LABEL_FONT_SIZE.toFloat(),
    val axisLabelFontFamily: FontFamily? = null, // null = 시스템 기본(iOS systemFont 대응)
    val axisLabelFontWeight: FontWeight? = null, // null = 기본 웨이트(regular)
    val axisLabelColor: Color,

    // 플롯 여백
    val plotInsets: Insets = Insets(
        top = ChartDefaults.PLOT_INSET_TOP.toFloat(),
        left = ChartDefaults.PLOT_INSET_LEFT.toFloat(),
        bottom = ChartDefaults.PLOT_INSET_BOTTOM.toFloat(),
        right = ChartDefaults.PLOT_INSET_RIGHT.toFloat(),
    ),

    // 터치 마커
    val touchLineColor: Color,
    val touchDotRadius: Float = ChartDefaults.TOUCH_DOT_RADIUS.toFloat(),

    // 데이터 색 role이 주입 맵(donutColors)에 없을 때의 폴백(iOS .systemGray).
    val fallbackDataColor: Color = Color(ChartDefaults.FALLBACK_DATA_COLOR),
) {
    companion object {
        /** 헤어라인 하한(px). 저밀도(1x)에서 0.5dp 그리드가 서브픽셀로 소실되는 것 방지(UX Minor-1). */
        internal const val HAIRLINE_MIN_PX = 1f

        /** 다크 여부에 따른 기본 스타일. [darkTheme] 판정은 호출부(`isSystemInDarkTheme()`)가 넘긴다. */
        fun defaults(darkTheme: Boolean): ChartStyle = if (darkTheme) Dark else Light

        // 팔레트 RGBA·수치의 단일 원본은 코어 ChartDefaults(B7) — 여기서는 Compose Color 변환만.
        private val Light: ChartStyle = run {
            val P = ChartDefaults.LightPalette
            ChartStyle(
            primaryLineColor = Color(P.PRIMARY_LINE),
            secondaryLineColor = Color(P.SECONDARY_LINE),
            gridLineColor = Color(P.GRID_LINE).copy(alpha = ChartDefaults.GRID_LINE_ALPHA.toFloat()),
            overlayLineColor = Color(P.OVERLAY_LINE).copy(alpha = ChartDefaults.OVERLAY_LINE_ALPHA.toFloat()),
            refBandColor = Color(P.REF_BAND).copy(alpha = ChartDefaults.REF_BAND_ALPHA.toFloat()),
            areaFillColor = Color(P.AREA_FILL).copy(alpha = ChartDefaults.AREA_FILL_ALPHA.toFloat()),
            markerLineColor = Color(P.MARKER_LINE),
            markerEmphasisLineColor = Color(P.MARKER_EMPHASIS_LINE),
            barReferenceLineColor = Color(P.BAR_REFERENCE_LINE).copy(alpha = ChartDefaults.BAR_REFERENCE_LINE_ALPHA.toFloat()),
            barSelectionLineColor = Color(P.BAR_SELECTION_LINE).copy(alpha = ChartDefaults.BAR_SELECTION_LINE_ALPHA.toFloat()),
            barCalloutBackgroundColor = Color(P.BAR_CALLOUT_BACKGROUND),
            barCalloutTextColor = Color(P.BAR_CALLOUT_TEXT),
            donutColors = mapOf(
                DonutColorRole.ZONE1 to Color(P.DONUT_ZONE1),
                DonutColorRole.ZONE2 to Color(P.DONUT_ZONE2).copy(alpha = ChartDefaults.DONUT_ZONE2_ALPHA.toFloat()),
                DonutColorRole.ZONE3 to Color(P.DONUT_ZONE3),
                DonutColorRole.ZONE4 to Color(P.DONUT_ZONE4),
                DonutColorRole.ZONE5 to Color(P.DONUT_ZONE5),
            ),
            donutEmptyColor = Color(P.DONUT_EMPTY).copy(alpha = ChartDefaults.DONUT_EMPTY_ALPHA.toFloat()),
            donutCenterLabelColor = Color(P.DONUT_CENTER_LABEL).copy(alpha = ChartDefaults.SECONDARY_LABEL_ALPHA.toFloat()),
            donutCenterPercentColor = Color(P.DONUT_CENTER_PERCENT),
            axisLabelColor = Color(P.AXIS_LABEL).copy(alpha = ChartDefaults.SECONDARY_LABEL_ALPHA.toFloat()),
            touchLineColor = Color(P.TOUCH_LINE),
            )
        }

        private val Dark: ChartStyle = run {
            val D = ChartDefaults.DarkPalette
            ChartStyle(
            primaryLineColor = Color(D.PRIMARY_LINE),
            secondaryLineColor = Color(D.SECONDARY_LINE),
            gridLineColor = Color(D.GRID_LINE).copy(alpha = ChartDefaults.GRID_LINE_ALPHA.toFloat()),
            overlayLineColor = Color(D.OVERLAY_LINE).copy(alpha = ChartDefaults.OVERLAY_LINE_ALPHA.toFloat()),
            refBandColor = Color(D.REF_BAND).copy(alpha = ChartDefaults.REF_BAND_ALPHA.toFloat()),
            areaFillColor = Color(D.AREA_FILL).copy(alpha = ChartDefaults.AREA_FILL_ALPHA.toFloat()),
            markerLineColor = Color(D.MARKER_LINE),
            markerEmphasisLineColor = Color(D.MARKER_EMPHASIS_LINE),
            barReferenceLineColor = Color(D.BAR_REFERENCE_LINE).copy(alpha = ChartDefaults.BAR_REFERENCE_LINE_ALPHA.toFloat()),
            barSelectionLineColor = Color(D.BAR_SELECTION_LINE).copy(alpha = ChartDefaults.BAR_SELECTION_LINE_ALPHA.toFloat()),
            barCalloutBackgroundColor = Color(D.BAR_CALLOUT_BACKGROUND),
            barCalloutTextColor = Color(D.BAR_CALLOUT_TEXT),
            donutColors = mapOf(
                DonutColorRole.ZONE1 to Color(D.DONUT_ZONE1),
                DonutColorRole.ZONE2 to Color(D.DONUT_ZONE2).copy(alpha = ChartDefaults.DONUT_ZONE2_ALPHA.toFloat()),
                DonutColorRole.ZONE3 to Color(D.DONUT_ZONE3),
                DonutColorRole.ZONE4 to Color(D.DONUT_ZONE4),
                DonutColorRole.ZONE5 to Color(D.DONUT_ZONE5),
            ),
            donutEmptyColor = Color(D.DONUT_EMPTY).copy(alpha = ChartDefaults.DONUT_EMPTY_ALPHA.toFloat()),
            donutCenterLabelColor = Color(D.DONUT_CENTER_LABEL).copy(alpha = ChartDefaults.SECONDARY_LABEL_ALPHA.toFloat()),
            donutCenterPercentColor = Color(D.DONUT_CENTER_PERCENT),
            axisLabelColor = Color(D.AXIS_LABEL).copy(alpha = ChartDefaults.SECONDARY_LABEL_ALPHA.toFloat()),
            touchLineColor = Color(D.TOUCH_LINE),
            )
        }
    }
}

/**
 * [ChartStyle]의 기하 값(선 폭·여백·반경·링·dash·코너·마커점)을 **dp → px**로 환산한 사본을 반환한다.
 *
 * ## 왜 필요한가 (UX Critical-1 / Arch m4)
 * iOS는 CoreGraphics가 pt 좌표라 시스템이 밀도로 자동 스케일하지만, Compose `DrawScope`는 **px 좌표**다.
 * [ChartStyle] 수치는 (shared-types 계약대로) **dp 의미의 Float**이므로, 렌더/히트테스트 직전 경계에서
 * [density]를 곱해 px로 바꾸지 않으면 2.75~3x 실기기에서 선·링·여백이 1/density로 앙상하게 그려진다.
 *
 * ## iOS 수치 패리티 유지
 * 순수 조립부·단위테스트는 이 함수를 거치지 않고 원본 [ChartStyle](density=1 전제)을 그대로 쓴다. 렌더러
 * composable만 `LocalDensity.current.density`로 스케일한 사본을 사용하므로, 1dp=1px인 테스트 수치(96개)는
 * 그대로 유지되고 실기기에서만 물리 크기가 iOS와 같아진다.
 *
 * 색·알파·비율(gradientMaxAlpha·areaHeightFraction)·`axisLabelFontSize`(sp는 [density]+글꼴배율을
 * TextMeasurer가 처리)는 스케일하지 않는다. 그리드 폭은 [ChartStyle.HAIRLINE_MIN_PX] 하한을 적용한다.
 */
internal fun ChartStyle.scaledForDensity(density: Float): ChartStyle {
    fun FloatArray.scaled(): FloatArray = FloatArray(size) { this[it] * density }
    return copy(
        lineWidth = lineWidth * density,
        gridLineDashPattern = gridLineDashPattern.scaled(),
        gridLineWidth = maxOf(gridLineWidth * density, ChartStyle.HAIRLINE_MIN_PX),
        overlayLineWidth = overlayLineWidth * density,
        refLineDashPattern = refLineDashPattern.scaled(),
        barCornerRadius = barCornerRadius * density,
        barMinHeight = barMinHeight * density,
        donutRingWidth = donutRingWidth * density,
        plotInsets = Insets(
            top = plotInsets.top * density,
            left = plotInsets.left * density,
            bottom = plotInsets.bottom * density,
            right = plotInsets.right * density,
        ),
        touchDotRadius = touchDotRadius * density,
    )
}
