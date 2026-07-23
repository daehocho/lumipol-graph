// iOS: ChartStyle.swift
package com.lumipol.graph.renderer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.lumipol.graph.model.BarColorRole
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
    val lineWidth: Float = 2f,
    val primaryLineColor: Color,
    val secondaryLineColor: Color,
    /**
     * primary 축 main 시리즈 라인색 기반 area 그라데이션의 시작 알파. 0이면 그라데이션 없음.
     * secondary 축은 fill 중첩으로 탁해지므로 그라데이션 없이 라인만 그린다.
     */
    val gradientMaxAlpha: Float = 0.25f,

    // 그리드 (X tick 세로선 + Y tick 가로선). null이면 그리드 없음.
    val gridLineColor: Color?,
    val gridLineDashPattern: FloatArray = floatArrayOf(3f, 3f),
    val gridLineWidth: Float = 0.5f,

    // 고스트(지난 러닝)
    val ghostLineColor: Color,
    val ghostLineWidth: Float = 1.5f,
    val ghostDashPattern: FloatArray = floatArrayOf(4f, 3f),

    // 오버레이(코어가 자체 정규화한 시리즈) — 축 라벨·그라데이션 없는 점선 라인
    val overlayLineColor: Color,
    val overlayLineWidth: Float = 1.5f,
    val overlayLineDashPattern: FloatArray = floatArrayOf(2f, 2f),

    // 기준선/밴드
    val refLineColor: Color,
    val refLineDashPattern: FloatArray = floatArrayOf(6f, 3f),
    val refBandColor: Color,

    // 배경 고도 실루엣 (장식 area — 축/스크럽 없음)
    val areaFillColor: Color,
    val areaHeightFraction: Float = 0.35f,

    // 구간(km) 마커
    val markerLineColor: Color,
    val markerEmphasisLineColor: Color,

    // 스플릿 막대
    val barColors: Map<BarColorRole, Color>,
    val barWidthRatio: Float = 0.6f,   // 슬롯 폭 대비 막대 폭(iOS slot*0.6)
    val partialBarAlpha: Float = 0.6f, // 부분 스플릿 막대 흐림
    val barCornerRadius: Float = 3f,
    val barShowYAxisLabels: Boolean = true, // false면 y틱 라벨 숨김(그리드·참조선은 유지)
    val barShowXAxisLabels: Boolean = true, // false면 x축 하단 라벨 숨김
    val barReferenceLineColor: Color,
    val barMinHeight: Float = 2f, // 가장 빠른(짧은) 막대도 최소 가시 높이
    val barDimOpacity: Float = 0.35f, // 롱프레스 선택 시 미선택 막대 흐림 배율(iOS barDimOpacity)
    /** 막대별 색 오버라이드. null이면 [defaultPaceColor](연속 팔레트) 사용. 앱은 stable 람다를 넘길 것(리컴포지션 방지). */
    val barColorProvider: ((BarPaceColorInput) -> Color)? = null,
    val barSelectionLineColor: Color, // 선택 막대 세로 가이드선(iOS label α0.55)
    val barCalloutBackgroundColor: Color, // 말풍선 배경(iOS .label)
    val barCalloutTextColor: Color, // 말풍선 텍스트(iOS .systemBackground)
    val barCalloutFontSize: Float = 12f, // 말풍선 폰트 크기(sp) — iOS systemFont(12, .semibold)
    val barCalloutFontWeight: FontWeight = FontWeight.SemiBold,

    // 심박존 도넛
    val donutColors: Map<DonutColorRole, Color>,
    val donutRingWidth: Float = 28f,
    val donutEmptyColor: Color,

    // 축 라벨 (iOS `axisLabelFont: UIFont` → 크기·패밀리·웨이트로 분해 보관, TextStyle 조립은 draw 경계.
    // 모든 라벨 TextLayer(축/마커/기준선/바)가 공유한다 — iOS도 전부 axisLabelFont 단일 폰트.)
    val axisLabelFontSize: Float = 10f,
    val axisLabelFontFamily: FontFamily? = null, // null = 시스템 기본(iOS systemFont 대응)
    val axisLabelFontWeight: FontWeight? = null, // null = 기본 웨이트(regular)
    val axisLabelColor: Color,

    // 플롯 여백
    val plotInsets: Insets = Insets(top = 16f, left = 44f, bottom = 20f, right = 44f),

    // 터치 마커
    val touchLineColor: Color,
    val touchDotRadius: Float = 4f,

    // 데이터 색 role이 주입 맵(barColors/donutColors)에 없을 때의 폴백(iOS .systemGray).
    val fallbackDataColor: Color = Color(0xFF8E8E93),
) {
    companion object {
        /** 헤어라인 하한(px). 저밀도(1x)에서 0.5dp 그리드가 서브픽셀로 소실되는 것 방지(UX Minor-1). */
        internal const val HAIRLINE_MIN_PX = 1f

        /** 다크 여부에 따른 기본 스타일. [darkTheme] 판정은 호출부(`isSystemInDarkTheme()`)가 넘긴다. */
        fun defaults(darkTheme: Boolean): ChartStyle = if (darkTheme) Dark else Light

        // Apple 시스템색 실측 RGB(sRGB) — 라이트/다크 쌍. 브랜드 데이터 색은 이 값을 고정 소유한다.
        private val Light: ChartStyle = ChartStyle(
            primaryLineColor = Color(0xFF007AFF),                    // systemBlue
            secondaryLineColor = Color(0xFFFF3B30),                  // systemRed
            gridLineColor = Color(0xFFD1D1D6).copy(alpha = 0.7f),    // systemGray4 α0.7
            ghostLineColor = Color(0xFF8E8E93).copy(alpha = 0.7f),   // systemGray α0.7
            overlayLineColor = Color(0xFFAF52DE).copy(alpha = 0.8f), // systemPurple α0.8
            refLineColor = Color(0xFFFF9500),                        // systemOrange
            refBandColor = Color(0xFFFF9500).copy(alpha = 0.12f),    // systemOrange α0.12
            areaFillColor = Color(0xFFC7C7CC).copy(alpha = 0.35f),   // systemGray3 α0.35
            markerLineColor = Color(0xFFD1D1D6),                     // systemGray4
            markerEmphasisLineColor = Color(0xFF8E8E93),             // systemGray
            barColors = mapOf(
                BarColorRole.FASTER to Color(0xFF34C759),            // systemGreen
                BarColorRole.ON_TARGET to Color(0xFF8E8E93),         // systemGray
                BarColorRole.SLOWER to Color(0xFFFF9500),            // systemOrange
            ),
            barReferenceLineColor = Color(0xFF000000).copy(alpha = 0.6f), // label α0.6
            barSelectionLineColor = Color(0xFF000000).copy(alpha = 0.55f), // label α0.55
            barCalloutBackgroundColor = Color(0xFF000000),                 // label
            barCalloutTextColor = Color(0xFFFFFFFF),                       // systemBackground
            donutColors = mapOf(
                DonutColorRole.ZONE1 to Color(0xFF007AFF),                     // systemBlue
                DonutColorRole.ZONE2 to Color(0xFF34C759).copy(alpha = 0.7f),  // systemGreen α0.7
                DonutColorRole.ZONE3 to Color(0xFFFFCC00),                     // systemYellow
                DonutColorRole.ZONE4 to Color(0xFFFF9500),                     // systemOrange
                DonutColorRole.ZONE5 to Color(0xFFFF3B30),                     // systemRed
            ),
            donutEmptyColor = Color(0xFFD1D1D6).copy(alpha = 0.5f),   // systemGray4 α0.5
            axisLabelColor = Color(0xFF3C3C43).copy(alpha = 0.6f),   // secondaryLabel
            touchLineColor = Color(0xFF000000),                      // label
        )

        private val Dark: ChartStyle = ChartStyle(
            primaryLineColor = Color(0xFF0A84FF),
            secondaryLineColor = Color(0xFFFF453A),
            gridLineColor = Color(0xFF3A3A3C).copy(alpha = 0.7f),
            ghostLineColor = Color(0xFF8E8E93).copy(alpha = 0.7f),
            overlayLineColor = Color(0xFFBF5AF2).copy(alpha = 0.8f),
            refLineColor = Color(0xFFFF9F0A),
            refBandColor = Color(0xFFFF9F0A).copy(alpha = 0.12f),
            areaFillColor = Color(0xFF48484A).copy(alpha = 0.35f),
            markerLineColor = Color(0xFF3A3A3C),
            markerEmphasisLineColor = Color(0xFF8E8E93),
            barColors = mapOf(
                BarColorRole.FASTER to Color(0xFF30D158),
                BarColorRole.ON_TARGET to Color(0xFF8E8E93),
                BarColorRole.SLOWER to Color(0xFFFF9F0A),
            ),
            barReferenceLineColor = Color(0xFFFFFFFF).copy(alpha = 0.6f),
            barSelectionLineColor = Color(0xFFFFFFFF).copy(alpha = 0.55f),
            barCalloutBackgroundColor = Color(0xFFFFFFFF),
            barCalloutTextColor = Color(0xFF000000),
            donutColors = mapOf(
                DonutColorRole.ZONE1 to Color(0xFF0A84FF),
                DonutColorRole.ZONE2 to Color(0xFF30D158).copy(alpha = 0.7f),
                DonutColorRole.ZONE3 to Color(0xFFFFD60A),
                DonutColorRole.ZONE4 to Color(0xFFFF9F0A),
                DonutColorRole.ZONE5 to Color(0xFFFF453A),
            ),
            donutEmptyColor = Color(0xFF3A3A3C).copy(alpha = 0.5f),
            axisLabelColor = Color(0xFFEBEBF5).copy(alpha = 0.6f),
            touchLineColor = Color(0xFFFFFFFF),
        )
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
        ghostLineWidth = ghostLineWidth * density,
        ghostDashPattern = ghostDashPattern.scaled(),
        overlayLineWidth = overlayLineWidth * density,
        overlayLineDashPattern = overlayLineDashPattern.scaled(),
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
