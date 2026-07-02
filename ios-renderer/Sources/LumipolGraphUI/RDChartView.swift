import UIKit
import LumipolGraph

/// KMP 코어 `LineChartData`를 받아 CAShapeLayer로 라인차트를 그리는 UIView.
/// 파이프라인: `LineChartEngine.layout` → `PlotArea`(픽셀 변환) → `ChartLayerBuilder`(레이어 조립).
@objc(RDChartView)
public final class RDChartView: UIView {

    /// main 라인 등장 애니메이션(strokeEnd 0→1). 스냅샷/테스트에서는 끈다.
    @objc public var isAnimationEnabled: Bool = true

    public override init(frame: CGRect) {
        super.init(frame: frame)
        installGestures()
    }

    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        installGestures()
    }

    private(set) var data: LineChartData?
    private(set) var chartLayout: LineChartLayout?
    private(set) var style: ChartStyle = .default
    private(set) var invertedAxes: Set<Axis> = []
    private(set) var labelFormatter: (ChartAxis, Double) -> String = RDChartView.defaultFormatter
    private(set) var seriesDisplayNames: [String: String] = [:]
    private(set) var currentPlotArea: PlotArea?
    private var chartLayers: [CALayer] = []
    private var touchMarkerLayer: CALayer?
    private var activeMarkerRawX: Double?
    private var needsEntranceAnimation = false

    /// 차트를 그린다. 터치 질의를 위해 `data`를 보관한다.
    ///
    /// 레이어는 즉시 만들어지지 않고 **다음 레이아웃 패스(`layoutSubviews`)에서 구축**된다.
    /// `render()` 직후 레이어가 필요하면(스냅샷 등) `layoutIfNeeded()`를 호출할 것.
    /// - Parameters:
    ///   - invertedAxes: 화면에서 뒤집을 Y축(예: 페이스 — 위=빠름). 코어 출력은 값-공간 그대로.
    ///   - labelFormatter: 축 tick 값 → 표시 문자열. 코어/렌더러는 단위를 모른다(앱 주입).
    ///   - seriesDisplayNames: 터치 말풍선에 쓸 seriesId → 표시명 (미지정 시 raw id).
    public func render(
        _ data: LineChartData,
        style: ChartStyle = .default,
        invertedAxes: Set<Axis> = [],
        labelFormatter: ((ChartAxis, Double) -> String)? = nil,
        seriesDisplayNames: [String: String] = [:]
    ) {
        hideTouchMarker()
        self.data = data
        self.style = style
        self.invertedAxes = invertedAxes
        self.labelFormatter = labelFormatter ?? RDChartView.defaultFormatter
        self.seriesDisplayNames = seriesDisplayNames
        self.chartLayout = LineChartEngine.shared.layout(data: data)
        needsEntranceAnimation = isAnimationEnabled
        setNeedsLayout()
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
        let markerRawX = activeMarkerRawX
        hideTouchMarker()
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
        if needsEntranceAnimation {
            animateMainLines()
            needsEntranceAnimation = false
        }
        if let markerRawX {
            showTouchMarker(atX: markerRawX)
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

    // MARK: - Touch marker

    /// 원본 도메인 x 위치에 근접점 마커(수직선+점+말풍선)를 표시한다.
    @objc public func showTouchMarker(atX rawX: Double) {
        guard let data, let chartLayout, let plotArea = currentPlotArea else { return }
        hideTouchMarker()
        let context = TouchMarker.Context(
            data: data, layout: chartLayout, style: style,
            plotArea: plotArea, formatter: labelFormatter,
            displayNames: seriesDisplayNames
        )
        guard let marker = TouchMarker.makeLayer(atRawX: rawX, context: context) else { return }
        layer.addSublayer(marker)
        touchMarkerLayer = marker
        activeMarkerRawX = rawX
    }

    @objc public func hideTouchMarker() {
        touchMarkerLayer?.removeFromSuperlayer()
        touchMarkerLayer = nil
        activeMarkerRawX = nil
    }

    private func installGestures() {
        addGestureRecognizer(UIPanGestureRecognizer(target: self, action: #selector(handleGesture(_:))))
        addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(handleGesture(_:))))
    }

    @objc private func handleGesture(_ recognizer: UIGestureRecognizer) {
        guard let chartLayout, let plotArea = currentPlotArea,
              let xTicks = chartLayout.axisTicks.first(where: { $0.axis == .x })?.ticks,
              let xScale = AxisScale(ticks: xTicks)
        else { return }
        let location = recognizer.location(in: self)
        let rawX = xScale.value(atPosition: plotArea.normalizedX(at: location.x))
        showTouchMarker(atX: rawX)
    }

    static func defaultFormatter(_ axis: ChartAxis, _ value: Double) -> String {
        String(format: "%g", value)
    }
}
