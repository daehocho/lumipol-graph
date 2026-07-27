import UIKit
import LumipolGraph

public protocol RDHeartRateZoneSelectionDelegate: AnyObject {
    /// 탭 토글 선택(0.26.0): 선택 확정·이동 시 원본 인덱스, 재탭·링 밖 탭·자동 해제 시 nil.
    /// render(데이터 교체)로 인한 리셋은 통지하지 않는다.
    func heartRateZoneView(_ view: RDHeartRateZoneView, didSelectSegmentAt index: Int?)
}

/// 심박존 분포 도넛. DonutEngine 레이아웃을 arc 스트로크로 렌더. 축/줌 없음.
/// 탭 토글 선택 → 센터 라벨(존 이름+퍼센트) + 비선택 디밍 + 자동 해제 + 햅틱(0.26.0).
@objc(RDHeartRateZoneView)
public final class RDHeartRateZoneView: UIView {

    public weak var zoneDelegate: RDHeartRateZoneSelectionDelegate?
    public private(set) var segmentLayers: [CAShapeLayer] = []
    /// 현재 선택된 **원본 data.segments 인덱스**. nil=선택 없음.
    public private(set) var selectedIndex: Int?

    let zoneNameLabel = UILabel()   // 센터 위줄: 존 이름(없으면 숨김)
    let percentLabel = UILabel()    // 센터 아래줄: 퍼센트

    private var data: DonutChartData = DonutChartData(segments: [])
    private var style: ChartStyle = .default
    private var currentLayout: DonutChartLayout?
    private var autoDeselectTimer: Timer?
    private let haptics = UIImpactFeedbackGenerator(style: .light)

    public override init(frame: CGRect) {
        super.init(frame: frame)
        setupCenterLabels()
    }

    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupCenterLabels()
    }

    deinit { autoDeselectTimer?.invalidate() }

    public func render(_ data: DonutChartData, style: ChartStyle = .default) {
        self.data = data
        self.style = style
        self.currentLayout = DonutEngine.shared.layout(data: data)
        // 데이터가 바뀌면 기존 인덱스는 무효 — 조용히 해제(통지 없음, 재렌더 루프 방지).
        clearSelection(notify: false)
        setNeedsLayout()
        layoutIfNeeded()
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        redraw()
        updateSelectionAppearance()
    }

    // MARK: - 그리기 (redraw/arcLayer는 기존 코드 그대로 유지)

    private func redraw() {
        segmentLayers.forEach { $0.removeFromSuperlayer() }
        segmentLayers.removeAll()

        let ring = style.donutRingWidth
        let radius = (min(bounds.width, bounds.height) - ring) / 2
        guard radius > 0 else { return }
        let center = CGPoint(x: bounds.midX, y: bounds.midY)
        let start = -CGFloat.pi / 2  // 12시 방향 시작, 시계방향

        guard let layout = currentLayout, layout.total > 0, !layout.segments.isEmpty else {
            let empty = arcLayer(center: center, radius: radius, ringWidth: ring,
                                 from: 0, to: 2 * .pi, color: style.donutEmptyColor)
            layer.addSublayer(empty)
            segmentLayers.append(empty)
            return
        }

        for seg in layout.segments {
            let a0 = start + 2 * .pi * CGFloat(seg.startFraction)
            let a1 = start + 2 * .pi * CGFloat(seg.startFraction + seg.sweepFraction)
            let color = style.donutColors[seg.colorRole] ?? .systemGray
            let shape = arcLayer(center: center, radius: radius, ringWidth: ring, from: a0, to: a1, color: color)
            layer.addSublayer(shape)
            segmentLayers.append(shape)
        }
    }

    private func arcLayer(center: CGPoint, radius: CGFloat, ringWidth: CGFloat,
                          from a0: CGFloat, to a1: CGFloat, color: UIColor) -> CAShapeLayer {
        let path = UIBezierPath(arcCenter: center, radius: radius, startAngle: a0, endAngle: a1, clockwise: true)
        let shape = CAShapeLayer()
        shape.path = path.cgPath
        shape.fillColor = UIColor.clear.cgColor
        shape.strokeColor = color.cgColor
        shape.lineWidth = ringWidth
        shape.lineCap = .butt
        return shape
    }

    // MARK: - 탭 토글

    public override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }
        handleTap(at: touch.location(in: self))
    }
    // touchesBegan/touchesCancelled 오버라이드 없음 — 토글 모델엔 "누르는 동안" 상태가 없어
    // 스크롤 가로챔에도 선택을 유지한다(기존 press-and-hold의 고착 문제가 원천적으로 없음).

    /// 탭 좌표 → 토글 전이 → 상태·표시·타이머·통지. 상태가 실제로 바뀔 때만 통지.
    func handleTap(at point: CGPoint) {
        let tapped = segmentIndex(at: point)
        let next = toggled(current: selectedIndex, tapped: tapped)
        guard next != selectedIndex else { return }
        if next != nil, style.donutSelectionHapticsEnabled {
            haptics.impactOccurred()
        }
        selectedIndex = next
        updateSelectionAppearance()
        scheduleAutoDeselect()
        zoneDelegate?.heartRateZoneView(self, didSelectSegmentAt: next)
    }

    /// 코어 전이 함수 브리지 — Int? ↔ KotlinInt?(ObjC export).
    private func toggled(current: Int?, tapped: Int?) -> Int? {
        DonutEngine.shared.toggleSelection(
            current: current.map { KotlinInt(value: Int32($0)) },
            tapped: tapped.map { KotlinInt(value: Int32($0)) }
        )?.intValue
    }

    private func scheduleAutoDeselect() {
        autoDeselectTimer?.invalidate()
        autoDeselectTimer = nil
        guard selectedIndex != nil, style.donutAutoDeselectDelay > 0 else { return }
        autoDeselectTimer = Timer.scheduledTimer(
            withTimeInterval: style.donutAutoDeselectDelay, repeats: false
        ) { [weak self] _ in
            self?.clearSelection(notify: true)
        }
    }

    private func clearSelection(notify: Bool) {
        autoDeselectTimer?.invalidate()
        autoDeselectTimer = nil
        let hadSelection = selectedIndex != nil
        selectedIndex = nil
        updateSelectionAppearance()
        if notify && hadSelection {
            zoneDelegate?.heartRateZoneView(self, didSelectSegmentAt: nil)
        }
    }

    // MARK: - 선택 표시(디밍 + 센터 라벨)

    private func setupCenterLabels() {
        for label in [zoneNameLabel, percentLabel] {
            label.textAlignment = .center
            label.lineBreakMode = .byTruncatingTail
            label.isHidden = true
            addSubview(label)
        }
        isAccessibilityElement = true
        accessibilityLabel = "심박존 도넛"
    }

    private func updateSelectionAppearance() {
        guard let layout = currentLayout, layout.total > 0 else {
            zoneNameLabel.isHidden = true
            percentLabel.isHidden = true
            accessibilityLabel = "심박존 도넛"
            return
        }
        // 디밍: 선택 중이면 비선택 조각의 alpha를 donutDimmedAlpha로 대체.
        for (i, seg) in layout.segments.enumerated() where i < segmentLayers.count {
            let base = style.donutColors[seg.colorRole] ?? .systemGray
            let dimmed = selectedIndex != nil && Int(seg.sourceIndex) != selectedIndex
            segmentLayers[i].strokeColor =
                dimmed ? base.withAlphaComponent(style.donutDimmedAlpha).cgColor : base.cgColor
        }
        guard let selected = selectedIndex,
              let seg = layout.segments.first(where: { Int($0.sourceIndex) == selected }) else {
            zoneNameLabel.isHidden = true
            percentLabel.isHidden = true
            accessibilityLabel = "심박존 도넛"
            return
        }
        let percentText = "\(Int((seg.sweepFraction * 100).rounded()))%"
        zoneNameLabel.font = style.donutCenterLabelFont
        zoneNameLabel.textColor = style.donutCenterLabelColor
        zoneNameLabel.text = seg.label
        zoneNameLabel.isHidden = (seg.label == nil)
        percentLabel.font = style.donutCenterPercentFont
        percentLabel.textColor = style.donutCenterPercentColor
        percentLabel.text = percentText
        percentLabel.isHidden = false
        accessibilityLabel = [seg.label, percentText].compactMap { $0 }.joined(separator: " ")
        layoutCenterLabels()
    }

    /// 센터 라벨 프레임: 내접원 90% 폭, 이름+퍼센트를 세로로 쌓아 중앙 정렬.
    private func layoutCenterLabels() {
        let ring = style.donutRingWidth
        let radius = (min(bounds.width, bounds.height) - ring) / 2
        let maxWidth = max(0, (radius - ring / 2) * 2 * 0.9)
        let nameH = zoneNameLabel.isHidden ? 0 : zoneNameLabel.font.lineHeight
        let pctH = percentLabel.font.lineHeight
        let totalH = nameH + pctH
        var y = bounds.midY - totalH / 2
        zoneNameLabel.frame = CGRect(x: bounds.midX - maxWidth / 2, y: y, width: maxWidth, height: nameH)
        y += nameH
        percentLabel.frame = CGRect(x: bounds.midX - maxWidth / 2, y: y, width: maxWidth, height: pctH)
    }

    // MARK: - 히트 테스트 (segmentIndex는 기존 코드 그대로 유지)

    /// 터치 좌표 → **원본 `data.segments` 인덱스**. 매칭 없으면 nil.
    /// DonutEngine은 value<=0 세그먼트를 레이아웃에서 제외하므로,
    /// 레이아웃 인덱스를 그대로 내보내면 호출자 배열과 어긋난다 — 원본 인덱스로 복원해 전달.
    func segmentIndex(at p: CGPoint) -> Int? {
        guard let layout = currentLayout, layout.total > 0 else { return nil }
        let center = CGPoint(x: bounds.midX, y: bounds.midY)
        // 반경 검사: 링 스트로크 대역 밖(도넛 구멍·모서리)은 선택 없음 —
        // 각도만으로는 모든 터치가 어떤 세그먼트에든 매칭돼 허위 선택이 된다.
        let ring = style.donutRingWidth
        let radius = (min(bounds.width, bounds.height) - ring) / 2
        guard radius > 0 else { return nil }
        let distance = hypot(p.x - center.x, p.y - center.y)
        guard distance >= radius - ring / 2, distance <= radius + ring / 2 else { return nil }
        var angle = atan2(p.y - center.y, p.x - center.x) + .pi / 2  // 12시 기준
        if angle < 0 { angle += 2 * .pi }
        let frac = Double(angle / (2 * .pi))
        guard let segment = layout.segments.first(where: {
            frac >= $0.startFraction && frac < $0.startFraction + $0.sweepFraction
        }) else { return nil }
        // 코어가 실어준 원본 인덱스를 그대로 보고 — value<=0 필터 규칙을 렌더러가 복제하지 않음
        // (엔진 규칙 변경에 자동 추종).
        return segment.sourceIndex >= 0 ? Int(segment.sourceIndex) : nil
    }
}
