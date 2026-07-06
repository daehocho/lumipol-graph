import UIKit

/// 차트 팔레트·타이포·여백. 앱이 통째로 주입해 커스터마이징한다(기본값 제공).
public struct ChartStyle {
    // 시리즈 라인
    public var lineWidth: CGFloat = 2
    public var primaryLineColor: UIColor = .systemBlue
    public var secondaryLineColor: UIColor = .systemRed
    /// primary 축 main 시리즈 라인색 기반 area 그라데이션의 시작 알파. 0이면 그라데이션 없음.
    /// secondary 축은 fill 중첩으로 탁해지므로 그라데이션 없이 라인만 그린다.
    public var gradientMaxAlpha: CGFloat = 0.25

    // 그리드 (X tick 세로선 + Y tick 가로선). nil이면 그리드 없음.
    public var gridLineColor: UIColor? = UIColor.systemGray4.withAlphaComponent(0.7)
    public var gridLineDashPattern: [NSNumber] = [3, 3]
    public var gridLineWidth: CGFloat = 0.5

    // 고스트(지난 러닝)
    public var ghostLineColor: UIColor = UIColor.systemGray.withAlphaComponent(0.7)
    public var ghostLineWidth: CGFloat = 1.5
    public var ghostDashPattern: [NSNumber] = [4, 3]

    // 기준선/밴드
    public var refLineColor: UIColor = .systemOrange
    public var refLineDashPattern: [NSNumber] = [6, 3]
    public var refBandColor: UIColor = UIColor.systemOrange.withAlphaComponent(0.12)

    // 배경 고도 실루엣 (장식 area — 축/스크럽 없음)
    public var areaFillColor: UIColor = UIColor.systemGray3.withAlphaComponent(0.35)
    public var areaHeightFraction: CGFloat = 0.35

    // 구간(km) 마커
    public var markerLineColor: UIColor = .systemGray4
    public var markerEmphasisLineColor: UIColor = .systemGray

    // 축 라벨
    public var axisLabelFont: UIFont = .systemFont(ofSize: 10)
    public var axisLabelColor: UIColor = .secondaryLabel

    // 플롯 여백 (좌우 = Y축 라벨, 상하 = 마커 라벨/X축 라벨)
    public var plotInsets: UIEdgeInsets = UIEdgeInsets(top: 16, left: 44, bottom: 20, right: 44)

    // 터치 마커
    public var touchLineColor: UIColor = .label
    public var touchDotRadius: CGFloat = 4
    public var bubbleBackgroundColor: UIColor = UIColor.label.withAlphaComponent(0.85)
    public var bubbleTextColor: UIColor = .systemBackground
    public var bubbleFont: UIFont = .systemFont(ofSize: 11, weight: .medium)

    public init() {}

    public static let `default` = ChartStyle()
}
