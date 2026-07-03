import UIKit
import LumipolGraph

/// 터치 지점의 근접점 마커 레이어(수직선 + 시리즈별 점 + 값 말풍선)를 만든다.
/// 근접 판정은 코어 `LineChartEngine.nearest`(원본 도메인 x) — 양 플랫폼 동일 로직.
enum TouchMarker {
    struct Context {
        let data: LineChartData
        let layout: LineChartLayout
        let style: ChartStyle
        let plotArea: PlotArea
        let formatter: (ChartAxis, Double) -> String
        /// seriesId → 말풍선 표시명 (없으면 raw id 노출)
        var displayNames: [String: String] = [:]
    }

    /// 원본 도메인 x 기준 마커 레이어. 표시 불가(플롯 영역 없음·축 변환 불능·근접점 없음)면 nil.
    static func makeLayer(atRawX rawX: Double, context: Context) -> CALayer? {
        guard context.plotArea.isRenderable,
              let xTicks = ticks(for: .x, in: context.layout),
              let xScale = AxisScale(ticks: xTicks)
        else { return nil }
        let results = LineChartEngine.shared.nearest(data: context.data, x: rawX)
        // 수직선 x는 첫 시리즈의 근접점 기준 — 계약상 시리즈별 근접 x가 다를 수 있으나
        // 러닝 데이터는 공통 샘플링 x를 쓰므로 하나로 통일한다.
        guard let snappedX = results.first?.x else { return nil }
        // 스냅된 근접점이 현재 표시 도메인(창) 밖이면 마커를 만들지 않는다 —
        // 확대 창 경계 부근에서 창 밖 데이터로 스냅되는 것을 방지.
        let rawNx = xScale.position(ofValue: snappedX)
        guard (0...1).contains(rawNx) else { return nil }
        let nx = rawNx
        let axisBySeriesId = Dictionary(uniqueKeysWithValues: context.data.series.map { ($0.id, $0.axis) })
        let roleBySeriesId = Dictionary(uniqueKeysWithValues: context.data.series.map { ($0.id, $0.role) })

        let container = CALayer()
        container.name = "touch.marker"

        let lineX = context.plotArea.x(nx)
        let line = CAShapeLayer()
        line.name = "touch.line"
        let linePath = UIBezierPath()
        linePath.move(to: CGPoint(x: lineX, y: context.plotArea.rect.minY))
        linePath.addLine(to: CGPoint(x: lineX, y: context.plotArea.rect.maxY))
        line.path = linePath.cgPath
        line.strokeColor = context.style.touchLineColor.cgColor
        line.lineWidth = 1
        container.addSublayer(line)

        var bubbleLines: [String] = []
        var topDotY = context.plotArea.rect.maxY
        for result in results {
            guard let axis = axisBySeriesId[result.seriesId],
                  let chartAxis = chartAxis(of: axis),
                  let yTicks = ticks(for: chartAxis, in: context.layout),
                  let yScale = AxisScale(ticks: yTicks)
            else { continue }
            let point = context.plotArea.point(
                NormalizedPoint(x: nx, y: yScale.position(ofValue: result.y)),
                axis: axis
            )
            let dot = CAShapeLayer()
            dot.name = "touch.dot.\(result.seriesId)"
            dot.path = UIBezierPath(
                arcCenter: point, radius: context.style.touchDotRadius,
                startAngle: 0, endAngle: .pi * 2, clockwise: true
            ).cgPath
            let dotColor: UIColor
            if roleBySeriesId[result.seriesId] == .ghost {
                dotColor = context.style.ghostLineColor
            } else if axis == .secondary {
                dotColor = context.style.secondaryLineColor
            } else {
                dotColor = context.style.primaryLineColor
            }
            dot.fillColor = dotColor.cgColor
            container.addSublayer(dot)
            topDotY = min(topDotY, point.y)
            let displayName = context.displayNames[result.seriesId] ?? result.seriesId
            bubbleLines.append("\(displayName) \(context.formatter(chartAxis, result.y))")
        }
        guard !bubbleLines.isEmpty else { return nil }
        container.addSublayer(
            bubbleLayer(lines: bubbleLines, anchorX: lineX, topY: topDotY, context: context)
        )
        return container
    }

    private static func ticks(for axis: ChartAxis, in layout: LineChartLayout) -> [AxisTick]? {
        layout.axisTicks.first { $0.axis == axis }?.ticks
    }

    private static func chartAxis(of axis: Axis) -> ChartAxis? {
        if axis == .primary { return .yPrimary }
        if axis == .secondary { return .ySecondary }
        return nil
    }

    private static func bubbleLayer(
        lines: [String], anchorX: CGFloat, topY: CGFloat, context: Context
    ) -> CALayer {
        let font = context.style.bubbleFont
        let padding: CGFloat = 6
        let lineHeight = ceil(font.lineHeight)
        let maxLineWidth = lines
            .map { ceil(($0 as NSString).size(withAttributes: [.font: font]).width) }
            .max() ?? 0
        let size = CGSize(
            width: maxLineWidth + padding * 2,
            height: lineHeight * CGFloat(lines.count) + padding * 2
        )
        var origin = CGPoint(x: anchorX - size.width / 2, y: topY - size.height - 8)
        origin.x = min(
            max(origin.x, context.plotArea.rect.minX),
            context.plotArea.rect.maxX - size.width
        )
        origin.y = max(origin.y, context.plotArea.rect.minY)

        let bubble = CALayer()
        bubble.name = "touch.bubble"
        bubble.frame = CGRect(origin: origin, size: size)
        bubble.backgroundColor = context.style.bubbleBackgroundColor.cgColor
        bubble.cornerRadius = 6
        for (index, text) in lines.enumerated() {
            let label = ChartLayerBuilder.textLayer(
                text, font: font, color: context.style.bubbleTextColor
            )
            label.frame.origin = CGPoint(x: padding, y: padding + lineHeight * CGFloat(index))
            bubble.addSublayer(label)
        }
        return bubble
    }
}
