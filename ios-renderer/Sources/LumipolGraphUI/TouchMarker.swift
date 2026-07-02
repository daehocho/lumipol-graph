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
    }

    /// 원본 도메인 x 기준 마커 레이어. 표시 불가(플롯 영역 없음·축 변환 불능·근접점 없음)면 nil.
    static func makeLayer(atRawX rawX: Double, context: Context) -> CALayer? {
        guard context.plotArea.isRenderable,
              let xTicks = ticks(for: .x, in: context.layout),
              let xScale = AxisScale(ticks: xTicks)
        else { return nil }
        let results = LineChartEngine.shared.nearest(data: context.data, x: rawX)
        guard let snappedX = results.first?.x else { return nil }
        let nx = min(max(xScale.position(ofValue: snappedX), 0), 1)
        let axisBySeriesId = Dictionary(uniqueKeysWithValues: context.data.series.map { ($0.id, $0.axis) })

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
            dot.fillColor = (axis == .secondary
                ? context.style.secondaryLineColor
                : context.style.primaryLineColor).cgColor
            container.addSublayer(dot)
            topDotY = min(topDotY, point.y)
            bubbleLines.append("\(result.seriesId) \(context.formatter(chartAxis, result.y))")
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
