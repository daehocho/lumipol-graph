import UIKit
import LumipolGraph

public protocol RDHeartRateZoneScrubDelegate: AnyObject {
    /// index=nil 이면 선택 해제.
    func heartRateZoneView(_ view: RDHeartRateZoneView, didSelectSegmentAt index: Int?)
}

/// 심박존 분포 도넛. DonutEngine 레이아웃을 arc 스트로크로 렌더. 축/줌 없음.
@objc(RDHeartRateZoneView)
public final class RDHeartRateZoneView: UIView {

    public weak var zoneDelegate: RDHeartRateZoneScrubDelegate?
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

    private func handleTouch(_ touch: UITouch?) {
        guard let touch = touch, let layout = currentLayout, layout.total > 0 else { return }
        let p = touch.location(in: self)
        let center = CGPoint(x: bounds.midX, y: bounds.midY)
        var angle = atan2(p.y - center.y, p.x - center.x) + .pi / 2  // 12시 기준
        if angle < 0 { angle += 2 * .pi }
        let frac = Double(angle / (2 * .pi))
        let idx = layout.segments.firstIndex { frac >= $0.startFraction && frac < $0.startFraction + $0.sweepFraction }
        zoneDelegate?.heartRateZoneView(self, didSelectSegmentAt: idx)
    }
}
