import UIKit
import LumipolGraph

/// 스플릿 막대 차트 뷰. 코어 BarChartLayout(정규화 완료)을 받아 CALayer로 그린다.
/// 페이스는 "낮을수록 빠름" — 막대 높이는 값 크기 그대로, 색으로 빠름/목표/느림을 전달한다.
public final class RDBarChartView: UIView {

    /// 솎아낸 이웃 라벨 사이 최소 여백(pt) — Android BAR_LABEL_MIN_GAP과 동일.
    private static let labelMinGap = 6.0

    public var style: ChartStyle = .default
    public private(set) var barLayers: [CALayer] = []
    public private(set) var selectedIndex: Int?
    private var selectionLayers: [CALayer] = []
    private let selectionFeedback = UISelectionFeedbackGenerator()

    private var layout: BarChartLayout?
    private var barLabels: [String]?
    private var xAxisLabels: [String]?
    private var yLabelFormatter: ((Double) -> String)?
    private let contentLayer = CALayer()

    public override init(frame: CGRect) {
        super.init(frame: frame)
        layer.addSublayer(contentLayer)
        installGestures()
    }
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        layer.addSublayer(contentLayer)
        installGestures()
    }

    private func installGestures() {
        let longPress = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
        longPress.minimumPressDuration = 0.5
        longPress.delegate = self
        addGestureRecognizer(longPress)
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
        selectedIndex = nil
        selectionLayers = []
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

        // 연속 색상 앵커(이 런 막대 value 기준). average = 등거리 스플릿에서 런 평균 페이스와 일치.
        // 색 앵커: 온전한 스플릿만 사용해 부분 스플릿(짧은 잔여 구간) 이상치가 팔레트를 왜곡하지 않게 한다.
        // 단, 온전한 스플릿이 2개 미만이거나 값이 모두 같아 범위가 없으면(예: 짧은 런의 단일 온전 스플릿)
        // 전체 막대로 폴백해 색 그라데이션 신호를 보존한다.
        let fullValues = layout.bars.filter { !$0.isPartial }.map { $0.value }
        let fullHasRange = fullValues.count >= 2 && fullValues.max()! > fullValues.min()!
        let anchorValues = fullHasRange ? fullValues : layout.bars.map { $0.value }
        let fastest = anchorValues.min() ?? 0
        let slowest = anchorValues.max() ?? 0
        let average = anchorValues.reduce(0, +) / Double(anchorValues.count)

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
            let colorInput = BarPaceColorInput(
                value: bar.value, fastest: fastest, slowest: slowest, average: average,
                isPartial: bar.isPartial, index: i, colorRole: bar.colorRole)
            let barColor = style.barColorProvider?(colorInput) ?? ChartStyle.defaultPaceColor(colorInput)
            barLayer.backgroundColor = barColor.cgColor
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

        applySelection()   // 레이아웃 재패스 중 선택 상태 유지
    }

    /// 롱프레스 선택 갱신. 값이 같으면 무시(중복 렌더·햅틱 방지).
    func selectBar(at index: Int?) {
        guard selectedIndex != index else { return }
        selectedIndex = index
        if index != nil { selectionFeedback.selectionChanged() }
        applySelection()
    }

    /// 손가락 뷰 좌표 → 막대 인덱스로 환산해 선택. barLabels(값 소스) 없으면 무시.
    func scrub(at location: CGPoint) {
        guard let layout = layout, !layout.bars.isEmpty, barLabels?.isEmpty == false else { return }
        let plot = bounds.inset(by: style.plotInsets)
        guard plot.width > 0,
              let idx = Self.barIndex(
                atX: location.x, plotMinX: plot.minX, plotWidth: plot.width, count: layout.bars.count)
        else { return }
        selectBar(at: idx)
    }

    @objc private func handleLongPress(_ recognizer: UILongPressGestureRecognizer) {
        switch recognizer.state {
        case .began:
            selectionFeedback.prepare()
            scrub(at: recognizer.location(in: self))
        case .changed:
            scrub(at: recognizer.location(in: self))
        case .ended, .cancelled, .failed:
            selectBar(at: nil)
        default:
            break
        }
    }

    /// 선택 상태를 기존 막대 레이어에 반영(재생성 없이 opacity만) + 말풍선 오버레이 교체.
    private func applySelection() {
        guard let layout = layout, barLayers.count == layout.bars.count else { return }
        CATransaction.begin()
        CATransaction.setDisableActions(true)   // 드래그 중 애니메이션 지연 방지
        defer { CATransaction.commit() }

        for (i, layer) in barLayers.enumerated() {
            let base: Float = layout.bars[i].isPartial ? style.barPartialOpacity : 1.0
            let dim = selectedIndex == nil || selectedIndex == i
            layer.opacity = dim ? base : base * style.barDimOpacity
        }

        selectionLayers.forEach { $0.removeFromSuperlayer() }
        selectionLayers = []
        guard let sel = selectedIndex, sel < barLayers.count else { return }

        let plot = bounds.inset(by: style.plotInsets)
        let barFrame = barLayers[sel].frame

        // 말풍선(페이스만) — barLabels 있을 때만
        if let labels = barLabels, sel < labels.count {
            let text = ChartLayerBuilder.textLayer(
                labels[sel], font: style.barCalloutFont,
                color: style.barCalloutTextColor.resolvedColor(with: traitCollection))
            let padH: CGFloat = 8, padV: CGFloat = 4, gap: CGFloat = 6
            let tSize = text.frame.size
            let bw = tSize.width + padH * 2
            let bh = tSize.height + padV * 2
            var bx = barFrame.midX - bw / 2
            bx = max(plot.minX, min(bx, plot.maxX - bw))   // 좌우 클램프(좌측 우선)
            var by = barFrame.minY - gap - bh
            if by < plot.minY { by = plot.minY }           // 상단 부족 시 클램프
            let bubbleRect = CGRect(x: bx, y: by, width: bw, height: bh)

            let bubble = CAShapeLayer()
            bubble.name = "bar.selection.bubble"
            bubble.frame = bubbleRect
            bubble.path = UIBezierPath(
                roundedRect: CGRect(origin: .zero, size: bubbleRect.size), cornerRadius: 6
            ).cgPath
            bubble.fillColor = style.barCalloutBackgroundColor.resolvedColor(with: traitCollection).cgColor
            contentLayer.addSublayer(bubble)
            selectionLayers.append(bubble)

            text.frame = CGRect(
                x: bx + padH, y: by + padV, width: tSize.width, height: tSize.height)
            contentLayer.addSublayer(text)
            selectionLayers.append(text)
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

    private func yTickLabel(_ value: Double) -> String {
        String(Int(value.rounded()))  // 앱이 barLabels/롱프레스 말풍선으로 표시 페이스를 다루므로 y틱은 원값(초)만
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

// MARK: - UIGestureRecognizerDelegate
extension RDBarChartView: UIGestureRecognizerDelegate {
    /// 세로 스크롤(UIScrollView) 안에서도 롱프레스 스크럽이 동작하도록 동시 인식 허용
    /// (형제 RDChartView와 동일 계약 — DGCharts drag-highlight 감각).
    public func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool { true }
}
