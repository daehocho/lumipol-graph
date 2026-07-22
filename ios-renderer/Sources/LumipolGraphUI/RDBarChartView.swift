import UIKit
import LumipolGraph

/// 스플릿 막대 차트 뷰. 코어 BarChartLayout(정규화 완료)을 받아 CALayer로 그린다.
/// 페이스는 "낮을수록 빠름" — 막대 높이는 값 크기 그대로, 색으로 빠름/목표/느림을 전달한다.
public final class RDBarChartView: UIView {

    /// 솎아낸 이웃 라벨 사이 최소 여백(pt) — Android BAR_LABEL_MIN_GAP과 동일.
    private static let labelMinGap = 6.0

    public var style: ChartStyle = .default
    public private(set) var barLayers: [CALayer] = []

    private var layout: BarChartLayout?
    private var barLabels: [String]?
    private var xAxisLabels: [String]?
    private var yLabelFormatter: ((Double) -> String)?
    private let contentLayer = CALayer()

    public override init(frame: CGRect) {
        super.init(frame: frame)
        layer.addSublayer(contentLayer)
    }
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        layer.addSublayer(contentLayer)
    }

    public func render(
        _ layout: BarChartLayout,
        style: ChartStyle = .default,
        barLabels: [String]? = nil,
        xAxisLabels: [String]? = nil,
        yLabelFormatter: ((Double) -> String)? = nil
    ) {
        self.layout = layout
        self.style = style
        self.barLabels = barLabels
        self.xAxisLabels = xAxisLabels
        self.yLabelFormatter = yLabelFormatter
        setNeedsLayout()
        layoutIfNeeded()  // layoutSubviews()→redraw()를 1회 유발 (테스트가 render 직후 barLayers 동기 접근)
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        contentLayer.frame = bounds
        redraw()
    }

    private func redraw() {
        contentLayer.sublayers?.forEach { $0.removeFromSuperlayer() }
        barLayers = []
        guard let layout = layout, !layout.bars.isEmpty, bounds.width > 0, bounds.height > 0 else { return }

        let insets = style.plotInsets
        let plot = bounds.inset(by: insets)
        guard plot.width > 0, plot.height > 0 else { return }

        // Y 그리드/틱 라벨
        for tick in layout.yTicks {
            let y = plot.maxY - CGFloat(tick.position) * plot.height
            if let grid = style.gridLineColor {
                let line = CAShapeLayer()
                let p = UIBezierPath()
                p.move(to: CGPoint(x: plot.minX, y: y))
                p.addLine(to: CGPoint(x: plot.maxX, y: y))
                line.path = p.cgPath
                line.strokeColor = grid.cgColor
                line.lineWidth = style.gridLineWidth
                line.lineDashPattern = style.gridLineDashPattern
                contentLayer.addSublayer(line)
            }
            if style.barShowYAxisLabels {
                let text = yLabelFormatter?(tick.value) ?? yTickLabel(tick.value)
                addLabel(text: text, at: CGPoint(x: insets.left - 4, y: y),
                         align: .right)
            }
        }

        // 막대
        let n = layout.bars.count
        let slot = plot.width / CGFloat(n)
        let barWidth = slot * 0.6

        // x축 인덱스 라벨 솎아내기 stride(장거리·하프 등 슬롯보다 넓은 라벨 겹침 방지).
        // 개수 임계치가 아니라 슬롯 폭 대비 라벨 폭으로 계산 — 코어 labelStride(양 플랫폼 공유).
        // 폭 측정은 CATextLayer를 만들지 않고 문자열 사이즈로 직접 구한다.
        let labelAttrs: [NSAttributedString.Key: Any] = [.font: style.axisLabelFont]
        func stride(for labels: [String]?) -> Int {
            guard let labels = labels, !labels.isEmpty else { return 1 }
            var maxW = 0.0
            for s in labels.prefix(n) {
                maxW = max(maxW, ceil((s as NSString).size(withAttributes: labelAttrs).width))
            }
            return Int(LabelThinningKt.labelStride(
                count: Int32(n), plotWidthPx: Double(plot.width), labelWidthPx: maxW, gapPx: Self.labelMinGap))
        }
        let xLabelStride = stride(for: xAxisLabels)

        for (i, bar) in layout.bars.enumerated() {
            let h = min(max(style.barMinHeight, CGFloat(bar.heightFraction) * plot.height), plot.height)
            let x = plot.minX + slot * CGFloat(i) + (slot - barWidth) / 2
            let rect = CGRect(x: x, y: plot.maxY - h, width: barWidth, height: h)
            let barLayer = CALayer()
            barLayer.frame = rect
            barLayer.cornerRadius = style.barCornerRadius
            barLayer.backgroundColor = color(for: bar.colorRole).cgColor
            if bar.isPartial { barLayer.opacity = 0.6 }  // 부분 스플릿은 흐리게
            contentLayer.addSublayer(barLayer)
            barLayers.append(barLayer)

            if style.barShowXAxisLabels, let xLabels = xAxisLabels, i < xLabels.count,
               LabelThinningKt.isLabelVisible(index: Int32(i), count: Int32(n), stride: Int32(xLabelStride)) {
                let baseline = plot.maxY + 4  // 막대 바닥 축선 아래 여백
                addLabel(text: xLabels[i], at: CGPoint(x: rect.midX, y: baseline), align: .topCenter)
            }
        }

        // 참조선(목표/평균)
        if let refBox = layout.referenceLinePosition {
            let refPos = CGFloat(truncating: refBox)
            let y = plot.maxY - refPos * plot.height
            let line = CAShapeLayer()
            let p = UIBezierPath()
            p.move(to: CGPoint(x: plot.minX, y: y))
            p.addLine(to: CGPoint(x: plot.maxX, y: y))
            line.path = p.cgPath
            line.strokeColor = style.barReferenceLineColor.cgColor
            line.lineWidth = 1
            line.lineDashPattern = style.refLineDashPattern
            contentLayer.addSublayer(line)
        }
    }

    /// 플롯 내 x(뷰 좌표)를 균등 슬롯 막대 인덱스로 변환. 경계 밖은 0..<count로 클램프.
    /// count/plotWidth가 0 이하면 nil. Android 착수 시 코어 barIndexAtX로 추출 예정.
    static func barIndex(atX x: CGFloat, plotMinX: CGFloat, plotWidth: CGFloat, count: Int) -> Int? {
        guard count > 0, plotWidth > 0 else { return nil }
        let slot = plotWidth / CGFloat(count)
        let raw = Int(((x - plotMinX) / slot).rounded(.down))
        return min(max(raw, 0), count - 1)
    }

    private func color(for role: BarColorRole) -> UIColor {
        style.barColors[role] ?? .systemGray
    }

    private func yTickLabel(_ value: Double) -> String {
        String(Int(value.rounded()))  // 앱이 barLabels로 표시 페이스를 주므로 y틱은 원값(초)만
    }

    private enum LabelAlign { case left, center, right, topCenter }
    private func addLabel(text: String, at point: CGPoint, align: LabelAlign) {
        let tl = ChartLayerBuilder.textLayer(text, font: style.axisLabelFont, color: style.axisLabelColor)
        let size = tl.frame.size
        var origin = point
        switch align {
        case .left: origin.y -= size.height / 2
        case .center: origin.x -= size.width / 2; origin.y -= size.height
        case .right: origin.x -= size.width; origin.y -= size.height / 2
        case .topCenter: origin.x -= size.width / 2
        }
        tl.frame = CGRect(origin: origin, size: size)
        contentLayer.addSublayer(tl)
    }
}
