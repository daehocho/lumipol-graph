package com.lumipol.graph

/**
 * 플랫폼 중립 정책 상수의 단일 원본(B7) — 렌더러 2벌이 수동 동기화하던 수치를 코어가 소유한다.
 *
 * 단위 규약: 길이·여백·반경·폰트 크기는 **논리 단위(dp/pt)** — px 환산(×density)은 렌더러 소관.
 * 시간은 초. 색은 `0xAARRGGBB` Long(알파 미리 안 곱힘 — 별도 `*_ALPHA` 상수를 렌더러가 곱한다.
 * 알파를 8비트로 미리 합성하면 기존 float 알파와 ±1/255 어긋나 스냅샷이 흔들린다).
 *
 * 렌더러는 이 값을 참조만 한다 — 신규 정책 수치 리터럴 금지(경계 정책 §4-3). 플랫폼 보정
 * 상수(헤어라인 하한·fontScale 상한 등)만 렌더러 잔류가 허용된다.
 */
object ChartDefaults {

    // ── 시리즈 라인 ──────────────────────────────────────────
    const val LINE_WIDTH: Double = 2.0
    const val OVERLAY_LINE_WIDTH: Double = 1.5
    const val GRADIENT_MAX_ALPHA: Double = 0.25

    // ── 그리드·기준선 dash ──────────────────────────────────
    const val GRID_LINE_WIDTH: Double = 0.5
    const val GRID_DASH_ON: Double = 3.0
    const val GRID_DASH_OFF: Double = 3.0
    const val REF_DASH_ON: Double = 6.0
    const val REF_DASH_OFF: Double = 3.0

    // ── 배경 고도 실루엣 ────────────────────────────────────
    const val AREA_HEIGHT_FRACTION: Double = 0.35
    const val AREA_MIN_VALUE_SPAN: Double = 0.5

    // ── 구간(km) 마커 ───────────────────────────────────────
    const val MARKER_LINE_WIDTH: Double = 1.0
    const val MARKER_EMPHASIS_LINE_WIDTH: Double = 1.5

    // ── 스플릿 막대 ─────────────────────────────────────────
    const val BAR_WIDTH_RATIO: Double = 0.6
    const val PARTIAL_BAR_ALPHA: Double = 0.6
    const val BAR_CORNER_RADIUS: Double = 3.0
    const val BAR_MIN_HEIGHT: Double = 2.0
    const val BAR_DIM_OPACITY: Double = 0.35
    const val BAR_CALLOUT_FONT_SIZE: Double = 12.0
    const val BAR_LABEL_GAP: Double = 4.0       // y틱 라벨과 축 사이
    const val BAR_X_LABEL_GAP: Double = 4.0     // x축 라벨과 막대 바닥 사이
    const val BAR_LABEL_MIN_GAP: Double = 6.0   // 솎아낸 이웃 라벨 사이 최소 여백
    const val BAR_X_UNIT_FONT_DELTA: Double = 2.0 // x축 단위 라벨(km/mi) 크기 가산 — 축 라벨 +2pt·볼드(0.44.0)
    const val CALLOUT_PAD_H: Double = 8.0
    const val CALLOUT_PAD_V: Double = 4.0
    const val CALLOUT_CORNER_RADIUS: Double = 6.0

    // ── 심박존 도넛 ─────────────────────────────────────────
    const val DONUT_RING_WIDTH: Double = 28.0
    const val DONUT_DIMMED_ALPHA: Double = 0.3
    const val DONUT_CENTER_LABEL_FONT_SIZE: Double = 13.0
    const val DONUT_CENTER_PERCENT_FONT_SIZE: Double = 28.0
    const val DONUT_AUTO_DESELECT_SECONDS: Double = 3.0
    const val DONUT_START_DEGREES: Double = -90.0   // 12시 시작(0°=3시)
    const val DONUT_CENTER_WIDTH_RATIO: Double = 0.9

    // ── 축 라벨·여백 ────────────────────────────────────────
    const val AXIS_LABEL_FONT_SIZE: Double = 10.0
    const val LABEL_GAP: Double = 2.0        // 마커/기준선 라벨과 선 사이
    const val AXIS_LABEL_GAP: Double = 4.0   // 축 라벨과 플롯 경계
    const val PLOT_INSET_TOP: Double = 16.0
    const val PLOT_INSET_LEFT: Double = 44.0
    const val PLOT_INSET_BOTTOM: Double = 20.0
    const val PLOT_INSET_RIGHT: Double = 44.0

    // ── 터치 마커 ───────────────────────────────────────────
    const val TOUCH_LINE_WIDTH: Double = 1.0
    const val TOUCH_DOT_RADIUS: Double = 4.0

    // ── 상호작용 ────────────────────────────────────────────
    const val MAX_ZOOM_SCALE: Double = 10.0

    /**
     * 스크럽 tick 햅틱 격자 — 손가락이 이 거리(iOS pt / AOS dp)를 넘어 이동할 때마다 1회.
     *
     * 1.1.1: 12.0 → 6.0 (12pt는 플롯 폭 302pt 기준 전구간 25회뿐 — "띄엄띄엄" 실사용 피드백).
     * 1.1.2: 6.0 → 0.3 (더 촘촘하게). 이 값에서 격자는 실질적으로 게이트 역할을 멈춘다 —
     * 손가락이 조금이라도 움직이면 매 프레임 통과하므로 밀도는 사실상
     * [SCRUB_HAPTIC_MIN_INTERVAL_MS] 단독으로 결정된다(초당 약 28회 연속). 더 촘촘하게 하려면
     * 이 값이 아니라 최소 간격을 내려야 하고, 그 지점부터는 개별 tick이 연속 진동으로 뭉개진다.
     *
     * 데이터 값(Y) 기준 tick은 채택하지 않았다 — 종합분석은 다중 시리즈라 기준 시리즈·단위를
     * 정해야 하고, 값이 평평한 구간에선 손가락을 움직여도 무진동이 되어 밀도 불만이 더 심해진다.
     */
    const val SCRUB_HAPTIC_SPACING_DP: Double = 0.3

    /** 스크럽 tick 최소 간격 — 빠른 플릭에서 진동이 연속음으로 뭉개지는 것을 막는 상한(≈28Hz). */
    const val SCRUB_HAPTIC_MIN_INTERVAL_MS: Long = 35

    // ── 등장 애니메이션 (구동은 플랫폼, 파라미터만 코어) ─────
    /** D5 결정(44 문서): 이징은 easeOut 통일 — CSS/CA `easeOut` cubic-bezier 계수. */
    const val ENTRANCE_EASING_X1: Double = 0.0
    const val ENTRANCE_EASING_Y1: Double = 0.0
    const val ENTRANCE_EASING_X2: Double = 0.58
    const val ENTRANCE_EASING_Y2: Double = 1.0
    /** D5 결정: 기본 off 통일 — 앱이 명시적으로 켠다. */
    const val ENTRANCE_ENABLED_DEFAULT: Boolean = false
    const val ENTRANCE_DURATION_SECONDS: Double = 0.6
    const val BAR_GROWTH_DURATION_SECONDS: Double = 0.3
    const val DONUT_SWEEP_DURATION_SECONDS: Double = 0.55

    // ── 폴백 색 ─────────────────────────────────────────────
    /** 데이터 색 role이 주입 맵에 없을 때 폴백(iOS .systemGray 실측). */
    const val FALLBACK_DATA_COLOR: Long = 0xFF8E8E93

    // ── 알파 (라이트/다크 공통 — 팔레트 base 색에 렌더러가 곱한다) ──
    const val GRID_LINE_ALPHA: Double = 0.7
    const val OVERLAY_LINE_ALPHA: Double = 0.8
    const val REF_BAND_ALPHA: Double = 0.12
    const val AREA_FILL_ALPHA: Double = 0.35
    const val BAR_REFERENCE_LINE_ALPHA: Double = 0.6
    const val BAR_SELECTION_LINE_ALPHA: Double = 0.55
    const val DONUT_ZONE2_ALPHA: Double = 0.7
    const val DONUT_EMPTY_ALPHA: Double = 0.5
    const val SECONDARY_LABEL_ALPHA: Double = 0.6

    /**
     * 라이트 팔레트 — Apple 시스템색 실측 RGB(sRGB). iOS 렌더러는 동적 UIColor를 유지하고
     * (다크 전환을 OS가 해석), AOS 렌더러가 이 값을 고정 소유한다. iOS 시스템색이 OS 업데이트로
     * 바뀌면 SceneDigest 해석값 대조(50 문서 §2)가 갈림을 드러낸다.
     */
    object LightPalette {
        const val PRIMARY_LINE: Long = 0xFF007AFF          // systemBlue
        const val SECONDARY_LINE: Long = 0xFFFF3B30        // systemRed
        const val GRID_LINE: Long = 0xFFD1D1D6             // systemGray4 (×GRID_LINE_ALPHA)
        const val OVERLAY_LINE: Long = 0xFFAF52DE          // systemPurple (×OVERLAY_LINE_ALPHA)
        const val REF_BAND: Long = 0xFFFF9500              // systemOrange (×REF_BAND_ALPHA)
        const val AREA_FILL: Long = 0xFFC7C7CC             // systemGray3 (×AREA_FILL_ALPHA)
        const val MARKER_LINE: Long = 0xFFD1D1D6           // systemGray4
        const val MARKER_EMPHASIS_LINE: Long = 0xFF8E8E93  // systemGray
        const val BAR_REFERENCE_LINE: Long = 0xFF000000    // label (×BAR_REFERENCE_LINE_ALPHA)
        const val BAR_SELECTION_LINE: Long = 0xFF000000    // label (×BAR_SELECTION_LINE_ALPHA)
        const val BAR_CALLOUT_BACKGROUND: Long = 0xFF000000 // label
        const val BAR_CALLOUT_TEXT: Long = 0xFFFFFFFF      // systemBackground
        const val DONUT_ZONE1: Long = 0xFF007AFF           // systemBlue
        const val DONUT_ZONE2: Long = 0xFF34C759           // systemGreen (×DONUT_ZONE2_ALPHA)
        const val DONUT_ZONE3: Long = 0xFFFFCC00           // systemYellow
        const val DONUT_ZONE4: Long = 0xFFFF9500           // systemOrange
        const val DONUT_ZONE5: Long = 0xFFFF3B30           // systemRed
        const val DONUT_EMPTY: Long = 0xFFD1D1D6           // systemGray4 (×DONUT_EMPTY_ALPHA)
        const val DONUT_CENTER_LABEL: Long = 0xFF3C3C43    // secondaryLabel (×SECONDARY_LABEL_ALPHA)
        const val DONUT_CENTER_PERCENT: Long = 0xFF000000  // label
        const val AXIS_LABEL: Long = 0xFF3C3C43            // secondaryLabel (×SECONDARY_LABEL_ALPHA)
        const val TOUCH_LINE: Long = 0xFF000000            // label
    }

    /** 다크 팔레트 — [LightPalette]와 동일 구조·동일 알파 규칙. */
    object DarkPalette {
        const val PRIMARY_LINE: Long = 0xFF0A84FF
        const val SECONDARY_LINE: Long = 0xFFFF453A
        const val GRID_LINE: Long = 0xFF3A3A3C
        const val OVERLAY_LINE: Long = 0xFFBF5AF2
        const val REF_BAND: Long = 0xFFFF9F0A
        const val AREA_FILL: Long = 0xFF48484A
        const val MARKER_LINE: Long = 0xFF3A3A3C
        const val MARKER_EMPHASIS_LINE: Long = 0xFF8E8E93
        const val BAR_REFERENCE_LINE: Long = 0xFFFFFFFF
        const val BAR_SELECTION_LINE: Long = 0xFFFFFFFF
        const val BAR_CALLOUT_BACKGROUND: Long = 0xFFFFFFFF
        const val BAR_CALLOUT_TEXT: Long = 0xFF000000
        const val DONUT_ZONE1: Long = 0xFF0A84FF
        const val DONUT_ZONE2: Long = 0xFF30D158
        const val DONUT_ZONE3: Long = 0xFFFFD60A
        const val DONUT_ZONE4: Long = 0xFFFF9F0A
        const val DONUT_ZONE5: Long = 0xFFFF453A
        const val DONUT_EMPTY: Long = 0xFF3A3A3C
        const val DONUT_CENTER_LABEL: Long = 0xFFEBEBF5
        const val DONUT_CENTER_PERCENT: Long = 0xFFFFFFFF
        const val AXIS_LABEL: Long = 0xFFEBEBF5
        const val TOUCH_LINE: Long = 0xFFFFFFFF
    }
}
