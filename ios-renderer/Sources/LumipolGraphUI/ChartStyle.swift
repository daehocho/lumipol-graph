import UIKit
import LumipolGraph

/// 차트 팔레트·타이포·여백. 앱이 통째로 주입해 커스터마이징한다(기본값 제공).
public struct ChartStyle {
    // 시리즈 라인
    public var lineWidth: CGFloat = CGFloat(ChartDefaults.shared.LINE_WIDTH)
    public var primaryLineColor: UIColor = .systemBlue
    public var secondaryLineColor: UIColor = .systemRed
    /// 선택된 **모든** 시리즈(축·역할 무관, overlay 포함)에 각자 색의 area 그라데이션을 그릴 때
    /// 시작 알파. 0이면 그라데이션 없음. 여러 장이 겹칠 때 탁해지지 않도록 실제 시작 알파는
    /// `gradientMaxAlpha / √n`(n=그라데이션 장수)으로 감쇠한다 — n=1이면 이 값 그대로.
    ///
    /// 감쇠의 한계: n은 실제 겹침이 아니라 **그리기 가능한 전체 시리즈 수**다. x 범위가 겹치지
    /// 않는 시리즈도 서로를 감쇠시키고(단독 구간이 α/√n로 흐려짐), 겹치는 구간의 합성 불투명도는
    /// `1-(1-α/√n)^n`로 n과 함께 서서히 는다(α=0.25: n=2→0.33, n=9→0.54). 선택 상한이 없으므로
    /// 동시 선택이 많은 화면은 이 값을 낮추거나 0으로 끄는 편이 안전하다.
    public var gradientMaxAlpha: CGFloat = CGFloat(ChartDefaults.shared.GRADIENT_MAX_ALPHA)
    /// 시리즈 id → 색. 지정되면 라인·배경 그라데이션이 이 색을 쓴다(축 슬롯 색보다 우선).
    /// 비어 있으면 종전대로 축/역할 기반 색(primary/secondary/overlay)으로 폴백한다.
    public var seriesColors: [String: UIColor] = [:]

    // 그리드 (X tick 세로선 + Y tick 가로선). nil이면 그리드 없음.
    public var gridLineColor: UIColor? = UIColor.systemGray4.withAlphaComponent(CGFloat(ChartDefaults.shared.GRID_LINE_ALPHA))
    public var gridLineDashPattern: [NSNumber] = [
        NSNumber(value: ChartDefaults.shared.GRID_DASH_ON), NSNumber(value: ChartDefaults.shared.GRID_DASH_OFF),
    ]
    public var gridLineWidth: CGFloat = CGFloat(ChartDefaults.shared.GRID_LINE_WIDTH)

    // 오버레이(코어가 자체 정규화한 시리즈) — 축 라벨 없는 가는 실선 라인(0.23.0부터 점선 제거).
    // 배경 그라데이션은 다른 시리즈와 동일하게 gradientMaxAlpha 규칙을 따른다(0.21.0부터).
    public var overlayLineColor: UIColor = UIColor.systemPurple.withAlphaComponent(CGFloat(ChartDefaults.shared.OVERLAY_LINE_ALPHA))
    public var overlayLineWidth: CGFloat = CGFloat(ChartDefaults.shared.OVERLAY_LINE_WIDTH)

    // 기준선/밴드 (refLineDashPattern은 BarChart 평균/목표 점선이 재사용)
    public var refLineDashPattern: [NSNumber] = [
        NSNumber(value: ChartDefaults.shared.REF_DASH_ON), NSNumber(value: ChartDefaults.shared.REF_DASH_OFF),
    ]
    public var refBandColor: UIColor = UIColor.systemOrange.withAlphaComponent(CGFloat(ChartDefaults.shared.REF_BAND_ALPHA))

    // 배경 고도 실루엣 (장식 area — 축/스크럽 없음)
    public var areaFillColor: UIColor = UIColor.systemGray3.withAlphaComponent(CGFloat(ChartDefaults.shared.AREA_FILL_ALPHA))
    public var areaHeightFraction: CGFloat = CGFloat(ChartDefaults.shared.AREA_HEIGHT_FRACTION)
    /// 실루엣 높이 정규화 분모의 하한(도메인 단위 — 고도면 m). 실측 고저차가 이보다 작으면
    /// 그만큼 납작하게 그려져 센서 노이즈가 산맥으로 보이지 않는다. 0이면 하한 없음.
    public var areaMinValueSpan: Double = ChartDefaults.shared.AREA_MIN_VALUE_SPAN

    // 구간(km) 마커
    public var markerLineColor: UIColor = .systemGray4
    public var markerEmphasisLineColor: UIColor = .systemGray

    // 스플릿 막대
    /// 슬롯 폭 대비 막대 폭 — 코어 정책 상수 기본(B7). 종전 iOS는 필드 없이 하드코딩(주입 비대칭).
    public var barWidthRatio: CGFloat = CGFloat(ChartDefaults.shared.BAR_WIDTH_RATIO)
    public var barCornerRadius: CGFloat = CGFloat(ChartDefaults.shared.BAR_CORNER_RADIUS)
    public var barShowYAxisLabels: Bool = true   // false면 y틱 라벨 숨김(그리드·참조선은 유지)
    public var barShowXAxisLabels: Bool = true   // false면 x축 하단 라벨(xAxisLabels) 숨김
    public var barReferenceLineColor: UIColor = UIColor.label.withAlphaComponent(CGFloat(ChartDefaults.shared.BAR_REFERENCE_LINE_ALPHA))
    public var barMinHeight: CGFloat = CGFloat(ChartDefaults.shared.BAR_MIN_HEIGHT)   // 가장 빠른(짧은) 막대도 최소 가시 높이
    public var barDimOpacity: Float = Float(ChartDefaults.shared.BAR_DIM_OPACITY)   // 롱프레스 선택 시 미선택 막대 흐림 배율
    public var barPartialOpacity: Float = Float(ChartDefaults.shared.PARTIAL_BAR_ALPHA)   // 부분 스플릿(마지막 조각) 막대 기본 흐림
    /// 막대별 색 오버라이드. nil이면 코어 PaceColormap(연속 팔레트) 사용.
    /// 단계적 폐기 예정(B6/C4) — 색약 모드는 colorBlindMode 주입으로 대체한다.
    public var barColorProvider: ((BarPaceColorInput) -> UIColor)?
    /// 색약 보정 모드 — 코어 컬러맵이 이산 4색(Okabe-Ito 계열)으로 전환(B6, D12).
    public var colorBlindMode: Bool = false
    public var barSelectionLineColor: UIColor = UIColor.label.withAlphaComponent(CGFloat(ChartDefaults.shared.BAR_SELECTION_LINE_ALPHA))
    public var barCalloutBackgroundColor: UIColor = .label
    public var barCalloutTextColor: UIColor = .systemBackground
    public var barCalloutFont: UIFont = .systemFont(ofSize: CGFloat(ChartDefaults.shared.BAR_CALLOUT_FONT_SIZE), weight: .semibold)

    // 심박존 도넛
    public var donutColors: [DonutColorRole: UIColor] = [
        .zone1: .systemBlue,
        .zone2: UIColor.systemGreen.withAlphaComponent(CGFloat(ChartDefaults.shared.DONUT_ZONE2_ALPHA)),
        .zone3: .systemYellow,
        .zone4: .systemOrange,
        .zone5: .systemRed,
    ]
    public var donutRingWidth: CGFloat = CGFloat(ChartDefaults.shared.DONUT_RING_WIDTH)
    public var donutEmptyColor: UIColor = UIColor.systemGray4.withAlphaComponent(CGFloat(ChartDefaults.shared.DONUT_EMPTY_ALPHA))

    // 심박존 도넛 — 탭 선택(0.26.0)
    public var donutDimmedAlpha: CGFloat = CGFloat(ChartDefaults.shared.DONUT_DIMMED_ALPHA)            // 비선택 세그먼트 alpha(원 alpha 대체)
    public var donutCenterLabelFont: UIFont = .systemFont(ofSize: CGFloat(ChartDefaults.shared.DONUT_CENTER_LABEL_FONT_SIZE))
    public var donutCenterLabelColor: UIColor = .secondaryLabel
    public var donutCenterPercentFont: UIFont = .systemFont(ofSize: CGFloat(ChartDefaults.shared.DONUT_CENTER_PERCENT_FONT_SIZE), weight: .bold)
    public var donutCenterPercentColor: UIColor = .label
    public var donutAutoDeselectDelay: TimeInterval = ChartDefaults.shared.DONUT_AUTO_DESELECT_SECONDS // 0 이하면 자동 해제 없음
    public var donutSelectionHapticsEnabled: Bool = true

    // 축 라벨
    public var axisLabelFont: UIFont = .systemFont(ofSize: CGFloat(ChartDefaults.shared.AXIS_LABEL_FONT_SIZE))
    public var axisLabelColor: UIColor = .secondaryLabel

    // 플롯 여백 (좌우 = Y축 라벨, 상하 = 마커 라벨/X축 라벨)
    public var plotInsets: UIEdgeInsets = UIEdgeInsets(
        top: CGFloat(ChartDefaults.shared.PLOT_INSET_TOP), left: CGFloat(ChartDefaults.shared.PLOT_INSET_LEFT),
        bottom: CGFloat(ChartDefaults.shared.PLOT_INSET_BOTTOM), right: CGFloat(ChartDefaults.shared.PLOT_INSET_RIGHT)
    )

    // 터치 마커 (말풍선 스타일은 bubble 레이어 제거와 함께 삭제됨 — 값 표시는 스크럽 델리게이트가 담당)
    public var touchLineColor: UIColor = .label
    public var touchDotRadius: CGFloat = CGFloat(ChartDefaults.shared.TOUCH_DOT_RADIUS)

    public init() {}

    public static let `default` = ChartStyle()
}
