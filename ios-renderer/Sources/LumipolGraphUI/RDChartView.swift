import UIKit
import LumipolGraph

/// KMP 코어 `LineChartData`를 받아 CAShapeLayer로 라인차트를 그리는 UIView.
/// 파이프라인: `LineChartEngine.layout` → `PlotArea`(픽셀 변환) → `ChartLayerBuilder`(레이어 조립).
@objc(RDChartView)
public final class RDChartView: UIView {

    /// main 라인 등장 애니메이션(strokeEnd 0→1). 스냅샷/테스트에서는 끈다.
    @objc public var isAnimationEnabled: Bool = true

    private(set) var data: LineChartData?
    private(set) var chartLayout: LineChartLayout?
    private(set) var style: ChartStyle = .default
    private(set) var invertedAxes: Set<Axis> = []
    private(set) var labelFormatter: (ChartAxis, Double) -> String = RDChartView.defaultFormatter
    private(set) var currentPlotArea: PlotArea?
    private var chartLayers: [CALayer] = []

    /// 차트를 그린다. 터치 질의를 위해 `data`를 보관한다.
    /// - Parameters:
    ///   - invertedAxes: 화면에서 뒤집을 Y축(예: 페이스 — 위=빠름). 코어 출력은 값-공간 그대로.
    ///   - labelFormatter: 축 tick 값 → 표시 문자열. 코어/렌더러는 단위를 모른다(앱 주입).
    public func render(
        _ data: LineChartData,
        style: ChartStyle = .default,
        invertedAxes: Set<Axis> = [],
        labelFormatter: ((ChartAxis, Double) -> String)? = nil
    ) {
        self.data = data
        self.style = style
        self.invertedAxes = invertedAxes
        self.labelFormatter = labelFormatter ?? RDChartView.defaultFormatter
        self.chartLayout = LineChartEngine.shared.layout(data: data)
        rebuildLayers()
    }

    /// ObjC 진입점 — 기본 스타일·반전 없음·기본 포매터.
    @objc public func render(data: LineChartData) {
        render(data)
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        rebuildLayers()
    }

    private func rebuildLayers() {
        chartLayers.forEach { $0.removeFromSuperlayer() }
        chartLayers = []
        currentPlotArea = nil
        guard let data, let chartLayout else { return }
        let plotArea = PlotArea(bounds: bounds, insets: style.plotInsets, invertedAxes: invertedAxes)
        guard plotArea.isRenderable else { return }
        currentPlotArea = plotArea
        let layers = ChartLayerBuilder.build(
            layout: chartLayout, data: data, style: style, plotArea: plotArea, formatter: labelFormatter
        )
        layers.forEach { layer.addSublayer($0) }
        chartLayers = layers
        if isAnimationEnabled {
            animateMainLines()
        }
    }

    private func animateMainLines() {
        for case let shape as CAShapeLayer in chartLayers
        where shape.name?.hasPrefix("series.main.") == true {
            let animation = CABasicAnimation(keyPath: "strokeEnd")
            animation.fromValue = 0
            animation.toValue = 1
            animation.duration = 0.6
            animation.timingFunction = CAMediaTimingFunction(name: .easeOut)
            shape.add(animation, forKey: "strokeEnd")
        }
    }

    static func defaultFormatter(_ axis: ChartAxis, _ value: Double) -> String {
        String(format: "%g", value)
    }
}
