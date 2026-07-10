import UIKit
import LumipolGraph

public protocol RDHeartRateZoneSelectionDelegate: AnyObject {
    /// index=nil 이면 선택 해제.
    func heartRateZoneView(_ view: RDHeartRateZoneView, didSelectSegmentAt index: Int?)
}

/// 심박존 분포 도넛. DonutEngine 레이아웃을 arc 스트로크로 렌더. 축/줌 없음.
@objc(RDHeartRateZoneView)
public final class RDHeartRateZoneView: UIView {

    public weak var zoneDelegate: RDHeartRateZoneSelectionDelegate?
    public private(set) var segmentLayers: [CAShapeLayer] = []

    private var data: DonutChartData = DonutChartData(segments: [])
    private var style: ChartStyle = .default
    private var currentLayout: DonutChartLayout?

    public func render(_ data: DonutChartData, style: ChartStyle = .default) {
        self.data = data
        self.style = style
        self.currentLayout = DonutEngine.shared.layout(data: data)
        setNeedsLayout()
        layoutIfNeeded()
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        redraw()
    }

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

    public override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        handleTouch(touches.first)
    }
    public override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        zoneDelegate?.heartRateZoneView(self, didSelectSegmentAt: nil)
    }
    /// 스크롤뷰 팬·시스템 제스처가 터치를 가로채면 touchesEnded 대신 이쪽이 온다 —
    /// 해제(nil)를 보내지 않으면 호스트의 존 하이라이트가 고착된다.
    public override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        zoneDelegate?.heartRateZoneView(self, didSelectSegmentAt: nil)
    }

    private func handleTouch(_ touch: UITouch?) {
        guard let touch = touch else { return }
        zoneDelegate?.heartRateZoneView(self, didSelectSegmentAt: segmentIndex(at: touch.location(in: self)))
    }

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
