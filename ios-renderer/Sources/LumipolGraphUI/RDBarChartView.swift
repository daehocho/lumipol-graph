import UIKit
import LumipolGraph

/// 스플릿 막대 차트 뷰. 코어 BarChartLayout(정규화 완료)을 받아 CALayer로 그린다.
/// 페이스는 "낮을수록 빠름" — 코어가 축을 반전해 빠른 스플릿일수록 막대가 길고 맨 위 틱이 가장 빠르다.
public final class RDBarChartView: UIView {

    /// 솎아낸 이웃 라벨 사이 최소 여백(pt) — 코어 정책 상수(B7, Android와 동일 원본).
    private static let labelMinGap = ChartDefaults.shared.BAR_LABEL_MIN_GAP

    public var style: ChartStyle = .default
    /// VoiceOver 낭독 문자열 주입(로컬라이즈는 앱 소유 — B12/D9). nil이면 코어 기본(ChartA11y.barChart).
    /// render 이후에 설정해도 즉시 반영된다(RDHeartRateZoneView와 동일 계약).
    public var accessibilityDescriptionOverride: String? {
        didSet { applyAccessibilityLabel() }
    }
    public private(set) var barLayers: [CALayer] = []
    public private(set) var selectedIndex: Int?
    private var selectionLayers: [CALayer] = []
    private let selectionFeedback = UISelectionFeedbackGenerator()

    private var layout: BarChartLayout?
    private var barLabels: [String]?
    private var xAxisLabels: [String]?
    private var xAxisUnitLabel: String?
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

    /// - Parameter xAxisUnitLabel: 그래프 오른쪽 하단 단위 라벨(km/mi 등). 기존 x축 라벨 줄은
    ///   밀리지 않고 플롯 오른쪽 끝(오른쪽 인셋 영역)에 덧붙는다. nil/빈 문자열이면 생략,
    ///   `barShowXAxisLabels`가 false면 함께 숨긴다.
    public func render(
        _ layout: BarChartLayout,
        style: ChartStyle = .default,
        barLabels: [String]? = nil,
        xAxisLabels: [String]? = nil,
        yLabelFormatter: ((Double) -> String)? = nil,
        xAxisUnitLabel: String? = nil
    ) {
        self.layout = layout
        self.style = style
        self.barLabels = barLabels
        self.xAxisLabels = xAxisLabels
        self.xAxisUnitLabel = xAxisUnitLabel
        self.yLabelFormatter = yLabelFormatter
        selectedIndex = nil
        selectionLayers = []
        // VoiceOver 요약(B12/D9 — 3차트 낭독을 SDK 기본으로). 문자열 규칙은 코어 ChartA11y.
        isAccessibilityElement = true
        applyAccessibilityLabel()
        setNeedsLayout()
        layoutIfNeeded()  // layoutSubviews()→redraw()를 1회 유발 (테스트가 render 직후 barLayers 동기 접근)
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        contentLayer.frame = bounds
        redraw()
    }

    /// 오버라이드 우선, 없으면 코어 기본 문자열. render 전(레이아웃 없음)에는 미적용 — 최초 render가 적용한다.
    private func applyAccessibilityLabel() {
        guard let layout else { return }
        accessibilityLabel = accessibilityDescriptionOverride
            ?? ChartA11y.shared.barChart(barCount: Int32(layout.bars.count), barLabels: barLabels ?? [])
    }

    private func redraw() {
        contentLayer.sublayers?.forEach { $0.removeFromSuperlayer() }
        barLayers = []
        guard let layout = layout, !layout.bars.isEmpty, bounds.width > 0, bounds.height > 0 else { return }

        let insets = style.plotInsets
        // B14: 라인차트와 동일한 PlotArea 경유 — Y 매핑 경로 통일. 막대 반전은 코어 값 그대로
        // (heightFraction·position이 이미 반전 축)이므로 invertedAxes는 비운다.
        let plotArea = PlotArea(bounds: bounds, insets: insets)
        guard plotArea.isRenderable else { return }
        let plot = plotArea.rect

        // Y 그리드/틱 라벨
        for tick in layout.yTicks {
            let y = plotArea.y(tick.position, axis: .primary)
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
                addLabel(text: text, at: CGPoint(x: insets.left - CGFloat(ChartDefaults.shared.BAR_LABEL_GAP), y: y),
                         align: .right)
            }
        }

        // 막대
        let n = layout.bars.count
        let slot = plot.width / CGFloat(n)
        let barWidth = slot * style.barWidthRatio

        // 연속 색상 앵커 — 규칙 원본은 코어 `BarChartEngine`(0.30.0, `layout.colorAnchors`).
        // 렌더러·앱이 각자 재계산하던 4벌 복제를 회수한 단일 원본이다. 코어를 안 거치고 직접
        // 생성된 레거시 layout(anchors=nil)만 국소 폴백으로 그린다(min/max/산술평균).
        let anchors: BarColorAnchors
        if let coreAnchors = layout.colorAnchors {
            anchors = coreAnchors
        } else {
            let values = layout.bars.map { $0.value }
            anchors = BarColorAnchors(
                fastest: values.min() ?? 0,
                slowest: values.max() ?? 0,
                average: values.reduce(0, +) / Double(max(values.count, 1))
            )
        }
        let fastest = anchors.fastest
        let slowest = anchors.slowest
        let average = anchors.average

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
            // B6: 기본 색은 코어 PaceColormap(0xAARRGGBB) — 렌더러는 UIColor 변환만.
            let barColor = style.barColorProvider?(colorInput)
                ?? UIColor(argb: PaceColormap.shared.rgba(
                    value: bar.value, anchors: anchors, colorBlind: style.colorBlindMode))
            barLayer.backgroundColor = barColor.cgColor
            contentLayer.addSublayer(barLayer)
            barLayers.append(barLayer)

            if style.barShowXAxisLabels, let xLabels = xAxisLabels, i < xLabels.count,
               LabelThinningKt.isLabelVisible(index: Int32(i), count: Int32(n), stride: Int32(xLabelStride)) {
                let baseline = plot.maxY + CGFloat(ChartDefaults.shared.BAR_X_LABEL_GAP)  // 막대 바닥 축선 아래 여백
                // 마지막 라벨 우측 클램프(0.47.0) — 넓은 라벨(42.2 등)이 플롯 끝을 넘어 단위
                // 라벨(km)과 겹치지 않게, 넘칠 때만 오른쪽 끝을 plot.maxX에 맞춰 왼쪽으로 민다.
                let text = xLabels[i]
                let labelWidth = ceil((text as NSString).size(withAttributes: labelAttrs).width)
                if i == n - 1, rect.midX + labelWidth / 2 > plot.maxX {
                    addLabel(text: text, at: CGPoint(x: plot.maxX, y: baseline), align: .topRight)
                } else {
                    addLabel(text: text, at: CGPoint(x: rect.midX, y: baseline), align: .topCenter)
                }
            }
        }

        // 그래프 오른쪽 하단 단위 라벨(km/mi 등, 0.43.0) — 플롯 오른쪽 끝(오른쪽 인셋 영역)에
        // 덧붙는다. 축 라벨보다 강조(볼드·+BAR_X_UNIT_FONT_DELTA, 0.44.0). 라벨 숨김이면 함께 숨긴다.
        if style.barShowXAxisLabels, let unit = xAxisUnitLabel, !unit.isEmpty {
            let baseline = plot.maxY + CGFloat(ChartDefaults.shared.BAR_X_LABEL_GAP)
            addLabel(text: unit,
                     at: CGPoint(x: plot.maxX + CGFloat(ChartDefaults.shared.BAR_LABEL_GAP), y: baseline),
                     align: .topLeft, font: xUnitFont())
        }

        // 참조선(목표/평균)
        if let refBox = layout.referenceLinePosition {
            let refPos = CGFloat(truncating: refBox)
            let y = plotArea.y(Double(refPos), axis: .primary)
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

    /// 선택 상태를 기존 막대 레이어에 반영(재생성 없이 opacity만) + 가이드선/말풍선 오버레이 교체.
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

        // 수직 가이드선(선택 막대 중앙, 플롯 상단~하단)
        let guide = CAShapeLayer()
        guide.name = "bar.selection.line"
        let gp = UIBezierPath()
        gp.move(to: CGPoint(x: barFrame.midX, y: plot.minY))
        gp.addLine(to: CGPoint(x: barFrame.midX, y: plot.maxY))
        guide.path = gp.cgPath
        guide.strokeColor = style.barSelectionLineColor.resolvedColor(with: traitCollection).cgColor
        guide.lineWidth = 1
        contentLayer.addSublayer(guide)
        selectionLayers.append(guide)

        // 말풍선(페이스만) — barLabels 있을 때만
        if let labels = barLabels, sel < labels.count {
            let text = ChartLayerBuilder.textLayer(
                labels[sel], font: style.barCalloutFont,
                color: style.barCalloutTextColor.resolvedColor(with: traitCollection))
            let padH = CGFloat(ChartDefaults.shared.CALLOUT_PAD_H), padV = CGFloat(ChartDefaults.shared.CALLOUT_PAD_V)
            let tSize = text.frame.size
            let bw = tSize.width + padH * 2
            let bh = tSize.height + padV * 2
            var bx = barFrame.midX - bw / 2
            bx = max(plot.minX, min(bx, plot.maxX - bw))   // 좌우 클램프(좌측 우선)
            // 손가락 가림 방지: 막대 높이와 무관하게 항상 플롯 상단에 고정(짧은 막대도 안 가려짐).
            let by = plot.minY
            let bubbleRect = CGRect(x: bx, y: by, width: bw, height: bh)

            let bubble = CAShapeLayer()
            bubble.name = "bar.selection.bubble"
            bubble.frame = bubbleRect
            bubble.path = UIBezierPath(
                roundedRect: CGRect(origin: .zero, size: bubbleRect.size),
                cornerRadius: CGFloat(ChartDefaults.shared.CALLOUT_CORNER_RADIUS)
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
    /// 코어 barIndexAtX로 위임(양 플랫폼 공유 — 히트테스트 슬롯 수학 일치).
    static func barIndex(atX x: CGFloat, plotMinX: CGFloat, plotWidth: CGFloat, count: Int) -> Int? {
        BarHitTestKt.barIndexAtX(
            x: Double(x), plotMinX: Double(plotMinX), plotWidth: Double(plotWidth), count: Int32(count)
        )?.intValue
    }

    private func yTickLabel(_ value: Double) -> String {
        String(Int(value.rounded()))  // 앱이 barLabels/롱프레스 말풍선으로 표시 페이스를 다루므로 y틱은 원값(초)만
    }

    /// x축 단위 라벨 폰트 — 앱이 주입한 축 라벨 폰트의 볼드 변형 +BAR_X_UNIT_FONT_DELTA pt.
    /// 볼드 트레이트가 없는 커스텀 폰트는 시스템 볼드로 폴백.
    private func xUnitFont() -> UIFont {
        let base = style.axisLabelFont
        let size = base.pointSize + CGFloat(ChartDefaults.shared.BAR_X_UNIT_FONT_DELTA)
        let traits = base.fontDescriptor.symbolicTraits.union(.traitBold)
        guard let descriptor = base.fontDescriptor.withSymbolicTraits(traits) else {
            return .boldSystemFont(ofSize: size)
        }
        return UIFont(descriptor: descriptor, size: size)
    }

    private enum LabelAlign { case left, center, right, topCenter, topLeft, topRight }
    private func addLabel(text: String, at point: CGPoint, align: LabelAlign, font: UIFont? = nil) {
        let tl = ChartLayerBuilder.textLayer(text, font: font ?? style.axisLabelFont, color: style.axisLabelColor)
        let size = tl.frame.size
        var origin = point
        switch align {
        case .left: origin.y -= size.height / 2
        case .center: origin.x -= size.width / 2; origin.y -= size.height
        case .right: origin.x -= size.width; origin.y -= size.height / 2
        case .topCenter: origin.x -= size.width / 2
        case .topLeft: break
        case .topRight: origin.x -= size.width
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
