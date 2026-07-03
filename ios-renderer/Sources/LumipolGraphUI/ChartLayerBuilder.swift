import UIKit
import LumipolGraph

/// 코어 `LineChartLayout`(정규화 0~1)을 `ChartStyle`·`PlotArea`로 CALayer 트리에 조립한다.
/// z-순서: 그리드 → 밴드 → 마커 → 고스트 → (그라데이션+main 라인) → 기준선 → 축 라벨.
enum ChartLayerBuilder {

    static func build(
        layout: LineChartLayout,
        data: LineChartData,
        style: ChartStyle,
        plotArea: PlotArea,
        formatter: (ChartAxis, Double) -> String
    ) -> [CALayer] {
        guard plotArea.isRenderable else { return [] }
        let axisBySeriesId = Dictionary(uniqueKeysWithValues: data.series.map { ($0.id, $0.axis) })
        var layers: [CALayer] = []

        if let gridColor = style.gridLineColor,
           let grid = gridLayer(layout: layout, color: gridColor, style: style, plotArea: plotArea) {
            layers.append(grid)
        }
        for (index, band) in layout.refBands.enumerated() {
            layers.append(bandLayer(band, index: index, style: style, plotArea: plotArea))
        }
        for (index, marker) in layout.markers.enumerated() {
            layers.append(markerLayer(marker, index: index, style: style, plotArea: plotArea))
        }
        for series in layout.series where series.role == .ghost {
            let axis = axisBySeriesId[series.id] ?? .primary
            if let layer = ghostLayer(series, axis: axis, style: style, plotArea: plotArea) {
                layers.append(layer)
            }
        }
        for series in layout.series where series.role == .main {
            let axis = axisBySeriesId[series.id] ?? .primary
            guard let path = linePath(series.points, axis: axis, plotArea: plotArea) else { continue }
            if style.gradientMaxAlpha > 0, axis == .primary {
                layers.append(gradientLayer(series, axis: axis, linePath: path, style: style, plotArea: plotArea))
            }
            layers.append(mainLineLayer(series, axis: axis, path: path, style: style))
        }
        for (index, refLine) in layout.refLines.enumerated() {
            layers.append(refLineLayer(refLine, index: index, style: style, plotArea: plotArea))
        }
        for ticksLayout in layout.axisTicks where !ticksLayout.ticks.isEmpty {
            layers.append(axisLabelsLayer(ticksLayout, style: style, plotArea: plotArea, formatter: formatter))
        }
        return layers
    }

    // MARK: - Series

    private static func linePath(_ points: [NormalizedPoint], axis: Axis, plotArea: PlotArea) -> UIBezierPath? {
        guard points.count >= 2 else { return nil }
        let path = UIBezierPath()
        path.move(to: plotArea.point(points[0], axis: axis))
        for point in points.dropFirst() {
            path.addLine(to: plotArea.point(point, axis: axis))
        }
        return path
    }

    private static func lineColor(for axis: Axis, style: ChartStyle) -> UIColor {
        axis == .secondary ? style.secondaryLineColor : style.primaryLineColor
    }

    private static func mainLineLayer(
        _ series: SeriesLayout, axis: Axis, path: UIBezierPath, style: ChartStyle
    ) -> CAShapeLayer {
        let layer = CAShapeLayer()
        layer.name = "series.main.\(series.id)"
        layer.path = path.cgPath
        layer.strokeColor = lineColor(for: axis, style: style).cgColor
        layer.fillColor = nil
        layer.lineWidth = style.lineWidth
        layer.lineJoin = .round
        layer.lineCap = .round
        return layer
    }

    private static func ghostLayer(
        _ series: SeriesLayout, axis: Axis, style: ChartStyle, plotArea: PlotArea
    ) -> CAShapeLayer? {
        guard let path = linePath(series.points, axis: axis, plotArea: plotArea) else { return nil }
        let layer = CAShapeLayer()
        layer.name = "series.ghost.\(series.id)"
        layer.path = path.cgPath
        layer.strokeColor = style.ghostLineColor.cgColor
        layer.fillColor = nil
        layer.lineWidth = style.ghostLineWidth
        layer.lineDashPattern = style.ghostDashPattern
        layer.lineJoin = .round
        return layer
    }

    private static func gradientLayer(
        _ series: SeriesLayout, axis: Axis, linePath: UIBezierPath, style: ChartStyle, plotArea: PlotArea
    ) -> CAGradientLayer {
        let color = lineColor(for: axis, style: style)
        let gradient = CAGradientLayer()
        gradient.name = "series.gradient.\(series.id)"
        gradient.frame = plotArea.rect
        gradient.colors = [
            color.withAlphaComponent(style.gradientMaxAlpha).cgColor,
            color.withAlphaComponent(0).cgColor,
        ]

        // 라인 아래(플롯 바닥까지)를 닫은 area path — 마스크는 gradient 로컬 좌표계라 원점만큼 평행이동.
        let areaPath = linePath.copy() as! UIBezierPath
        let firstPoint = plotArea.point(series.points[0], axis: axis)
        let lastPoint = plotArea.point(series.points[series.points.count - 1], axis: axis)
        areaPath.addLine(to: CGPoint(x: lastPoint.x, y: plotArea.rect.maxY))
        areaPath.addLine(to: CGPoint(x: firstPoint.x, y: plotArea.rect.maxY))
        areaPath.close()
        var translation = CGAffineTransform(translationX: -plotArea.rect.minX, y: -plotArea.rect.minY)
        let mask = CAShapeLayer()
        mask.path = areaPath.cgPath.copy(using: &translation)
        gradient.mask = mask
        return gradient
    }

    // MARK: - Grid

    /// X tick 세로선 + Y tick 가로선 점선 그리드. 가로선은 primary 축 tick 기준(없으면 secondary).
    private static func gridLayer(
        layout: LineChartLayout, color: UIColor, style: ChartStyle, plotArea: PlotArea
    ) -> CAShapeLayer? {
        func ticks(_ axis: ChartAxis) -> [AxisTick] {
            layout.axisTicks.first { $0.axis == axis }?.ticks ?? []
        }
        let path = UIBezierPath()
        for tick in ticks(.x) {
            let x = plotArea.x(tick.position)
            path.move(to: CGPoint(x: x, y: plotArea.rect.minY))
            path.addLine(to: CGPoint(x: x, y: plotArea.rect.maxY))
        }
        let primaryTicks = ticks(.yPrimary)
        let (yTicks, yAxis): ([AxisTick], Axis) =
            primaryTicks.isEmpty ? (ticks(.ySecondary), .secondary) : (primaryTicks, .primary)
        for tick in yTicks {
            let y = plotArea.y(tick.position, axis: yAxis)
            path.move(to: CGPoint(x: plotArea.rect.minX, y: y))
            path.addLine(to: CGPoint(x: plotArea.rect.maxX, y: y))
        }
        guard !path.isEmpty else { return nil }
        let layer = CAShapeLayer()
        layer.name = "grid"
        layer.path = path.cgPath
        layer.strokeColor = color.cgColor
        layer.fillColor = nil
        layer.lineWidth = style.gridLineWidth
        layer.lineDashPattern = style.gridLineDashPattern
        return layer
    }

    // MARK: - Reference / Marker

    private static func bandLayer(
        _ band: RefBandLayout, index: Int, style: ChartStyle, plotArea: PlotArea
    ) -> CALayer {
        let y1 = plotArea.y(band.lower, axis: band.axis)
        let y2 = plotArea.y(band.upper, axis: band.axis)
        let layer = CALayer()
        layer.name = "band.\(index)"
        layer.frame = CGRect(
            x: plotArea.rect.minX, y: min(y1, y2),
            width: plotArea.rect.width, height: abs(y1 - y2)
        )
        layer.backgroundColor = style.refBandColor.cgColor
        return layer
    }

    private static func markerLayer(
        _ marker: MarkerLayout, index: Int, style: ChartStyle, plotArea: PlotArea
    ) -> CALayer {
        let container = CALayer()
        container.name = "marker.\(index)"
        let x = plotArea.x(marker.position)
        let line = CAShapeLayer()
        let path = UIBezierPath()
        path.move(to: CGPoint(x: x, y: plotArea.rect.minY))
        path.addLine(to: CGPoint(x: x, y: plotArea.rect.maxY))
        line.path = path.cgPath
        line.strokeColor = (marker.emphasis ? style.markerEmphasisLineColor : style.markerLineColor).cgColor
        line.lineWidth = marker.emphasis ? 1.5 : 1
        container.addSublayer(line)
        if let label = marker.label {
            let text = textLayer(label, font: style.axisLabelFont, color: style.axisLabelColor)
            text.frame.origin = CGPoint(
                x: x - text.frame.width / 2,
                y: plotArea.rect.minY - text.frame.height - 2
            )
            container.addSublayer(text)
        }
        return container
    }

    private static func refLineLayer(
        _ refLine: RefLineLayout, index: Int, style: ChartStyle, plotArea: PlotArea
    ) -> CALayer {
        let container = CALayer()
        container.name = "refLine.\(index)"
        let y = plotArea.y(refLine.position, axis: refLine.axis)
        let line = CAShapeLayer()
        let path = UIBezierPath()
        path.move(to: CGPoint(x: plotArea.rect.minX, y: y))
        path.addLine(to: CGPoint(x: plotArea.rect.maxX, y: y))
        line.path = path.cgPath
        line.strokeColor = style.refLineColor.cgColor
        line.lineWidth = 1
        line.lineDashPattern = style.refLineDashPattern
        container.addSublayer(line)
        if let label = refLine.label {
            let text = textLayer(label, font: style.axisLabelFont, color: style.refLineColor)
            text.frame.origin = CGPoint(
                x: plotArea.rect.maxX - text.frame.width,
                y: y - text.frame.height - 2
            )
            container.addSublayer(text)
        }
        return container
    }

    // MARK: - Axis labels

    private static func axisLabelsLayer(
        _ ticksLayout: AxisTicksLayout, style: ChartStyle, plotArea: PlotArea,
        formatter: (ChartAxis, Double) -> String
    ) -> CALayer {
        let container = CALayer()
        container.name = "axisLabels.\(name(of: ticksLayout.axis))"
        for tick in ticksLayout.ticks {
            let text = textLayer(
                formatter(ticksLayout.axis, tick.value),
                font: style.axisLabelFont, color: style.axisLabelColor
            )
            let size = text.frame.size
            if ticksLayout.axis == .x {
                text.frame.origin = CGPoint(
                    x: plotArea.x(tick.position) - size.width / 2,
                    y: plotArea.rect.maxY + 4
                )
            } else if ticksLayout.axis == .yPrimary {
                let y = plotArea.y(tick.position, axis: .primary)
                text.frame.origin = CGPoint(
                    x: plotArea.rect.minX - size.width - 4,
                    y: y - size.height / 2
                )
            } else {
                let y = plotArea.y(tick.position, axis: .secondary)
                text.frame.origin = CGPoint(x: plotArea.rect.maxX + 4, y: y - size.height / 2)
            }
            container.addSublayer(text)
        }
        return container
    }

    private static func name(of axis: ChartAxis) -> String {
        if axis == .x { return "x" }
        if axis == .yPrimary { return "yPrimary" }
        return "ySecondary"
    }

    // MARK: - Text

    static func textLayer(_ text: String, font: UIFont, color: UIColor) -> CATextLayer {
        let layer = CATextLayer()
        layer.string = text
        layer.font = font // CATextLayer.font는 UIFont 인스턴스를 직접 수용한다
        layer.fontSize = font.pointSize
        layer.foregroundColor = color.cgColor
        layer.contentsScale = UIScreen.main.scale
        let size = (text as NSString).size(withAttributes: [.font: font])
        layer.frame = CGRect(origin: .zero, size: CGSize(width: ceil(size.width), height: ceil(size.height)))
        return layer
    }
}
