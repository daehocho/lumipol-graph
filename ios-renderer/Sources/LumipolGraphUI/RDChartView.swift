import UIKit
import LumipolGraph

/// KMP 코어 `LineChartData`를 받아 CAShapeLayer로 라인차트를 그리는 UIView.
/// 파이프라인: `LineChartEngine.layout` → `PlotArea`(픽셀 변환) → `ChartLayerBuilder`(레이어 조립).
@objc(RDChartView)
public final class RDChartView: UIView {

    /// main 라인 등장 애니메이션(strokeEnd 0→1). 스냅샷/테스트에서는 끈다.
    @objc public var isAnimationEnabled: Bool = true

    // MARK: - Zoom (X축 확대/팬)

    /// X축 핀치 줌 + 확대 상태 좌우 팬. 기본 꺼짐 — 기존 사용처 무영향.
    ///
    /// 더블탭 recognizer는 줌 활성화 시에만 뷰에 부착한다 — 항상 부착해 두면
    /// 단일 탭(마커)의 require(toFail:)가 비활성 더블탭의 실패 전이를 기다리다
    /// 영구 차단될 수 있다 (비활성 recognizer 대상 failure requirement는 문서상 보장 없음).
    @objc public var isZoomEnabled: Bool = false {
        didSet {
            guard isZoomEnabled != oldValue else { return }
            pinchRecognizer.isEnabled = isZoomEnabled
            if isZoomEnabled {
                addGestureRecognizer(doubleTapRecognizer)
            } else {
                removeGestureRecognizer(doubleTapRecognizer)
                resetZoom()
            }
        }
    }
    /// 최대 확대 배율 (전체 구간 대비)
    @objc public var maxZoomScale: CGFloat = 10

    private var zoomState: ZoomState?
    private let pinchRecognizer = UIPinchGestureRecognizer()
    private let doubleTapRecognizer = UITapGestureRecognizer()
    private let markerTapRecognizer = UITapGestureRecognizer()

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
    // 줌 변환 대상 콘텐츠(시리즈·그리드·밴드·마커·기준선)와 클립 컨테이너.
    // 축 라벨·터치 마커는 루트에 남아 변환/클립을 받지 않는다.
    private let clipContainer = CALayer()
    private let contentContainer = CALayer()
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
        zoomState = nil
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
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        defer { CATransaction.commit() }

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
        clipContainer.name = "zoom.clip"
        clipContainer.frame = bounds
        contentContainer.name = "zoom.content"
        contentContainer.frame = bounds
        contentContainer.setAffineTransform(.identity)
        contentContainer.sublayers?.forEach { $0.removeFromSuperlayer() }
        for built in layers {
            if built.name?.hasPrefix("axisLabels.") == true {
                layer.addSublayer(built)
            } else {
                contentContainer.addSublayer(built)
            }
        }
        clipContainer.addSublayer(contentContainer)
        layer.addSublayer(clipContainer)
        chartLayers = layers
        if needsEntranceAnimation {
            animateMainLines()
            needsEntranceAnimation = false
        }
        if let markerRawX {
            showTouchMarker(atX: markerRawX)
        }
        updateClipMask()
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

    // MARK: - Zoom

    /// 프로그래매틱 줌. isZoomEnabled가 꺼져 있으면 무시.
    public func zoom(toXRange range: ClosedRange<Double>) {
        guard isZoomEnabled, range.upperBound > range.lowerBound else { return }
        ensureZoomState()
        zoomState?.setWindow(range)
        commitViewport()
    }

    @objc public func resetZoom() {
        guard zoomState != nil else { return }
        zoomState = nil
        commitViewport()
    }

    /// 현재 표시 도메인(전체 layout의 X tick 양끝)으로 줌 상태를 초기화.
    /// zoomState가 nil일 때만 유효 — nil ⇒ 현재 layout이 전체 구간이라는 불변식.
    private func ensureZoomState() {
        guard zoomState == nil, let chartLayout,
              let xTicks = chartLayout.axisTicks.first(where: { $0.axis == .x })?.ticks,
              let scale = AxisScale(ticks: xTicks)
        else { return }
        let lower = scale.value(atPosition: 0)
        let upper = scale.value(atPosition: 1)
        guard upper > lower else { return }
        zoomState = ZoomState(fullDomain: lower...upper)
    }

    /// 제스처 종료/프로그래매틱 변경 시 — 창에 맞춰 코어 재계산 + 레이어 재조립 (하이브리드의 커밋 단계).
    private func commitViewport() {
        guard let data else { return }
        if let state = zoomState, state.isZoomed {
            chartLayout = LineChartEngine.shared.layout(
                data: data,
                xMin: state.window.lowerBound,
                xMax: state.window.upperBound
            )
        } else {
            zoomState = nil  // 1x 복귀 시 상태 제거 (ensureZoomState 불변식 유지)
            chartLayout = LineChartEngine.shared.layout(data: data)
        }
        needsEntranceAnimation = false
        setNeedsLayout()
        layoutIfNeeded()
        updateClipMask()
    }

    /// 줌 상태(확대 or 제스처 진행 중)일 때만 플롯 영역 클립.
    /// 마스크 위쪽을 뷰 상단까지 열어 구간(km) 마커 라벨은 잘리지 않게 한다.
    private func updateClipMask(gestureActive: Bool = false) {
        guard let plotArea = currentPlotArea else { return }
        let active = gestureActive || (zoomState?.isZoomed == true)
        if active {
            let mask = CAShapeLayer()
            mask.path = UIBezierPath(rect: CGRect(
                x: plotArea.rect.minX, y: 0,
                width: plotArea.rect.width, height: plotArea.rect.maxY
            )).cgPath
            clipContainer.mask = mask
        } else {
            clipContainer.mask = nil
        }
    }

    /// 제스처 진행 중 임시 X 변환 (하이브리드의 변환 단계 — 커밋 전 미리보기)
    private func applyLiveTransform(scaleX: CGFloat, translationX: CGFloat, anchorX: CGFloat) {
        var transform = CGAffineTransform(translationX: anchorX, y: 0)
        transform = transform.scaledBy(x: scaleX, y: 1)
        transform = transform.translatedBy(x: -anchorX, y: 0)
        transform = transform.concatenating(CGAffineTransform(translationX: translationX, y: 0))
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        contentContainer.setAffineTransform(transform)
        CATransaction.commit()
    }

    @objc private func handlePinch(_ recognizer: UIPinchGestureRecognizer) {
        guard isZoomEnabled, let plotArea = currentPlotArea, data != nil else { return }
        switch recognizer.state {
        case .began:
            ensureZoomState()
            hideTouchMarker()
            updateClipMask(gestureActive: true)
        case .changed:
            applyLiveTransform(
                scaleX: max(recognizer.scale, 0.01), translationX: 0,
                anchorX: recognizer.location(in: self).x
            )
        case .ended, .cancelled:
            let anchor = plotArea.normalizedX(at: recognizer.location(in: self).x)
            zoomState?.pinch(
                by: Double(recognizer.scale), anchor: anchor,
                maxScale: Double(maxZoomScale)
            )
            commitViewport()
        default:
            break
        }
    }

    @objc private func handleDoubleTap(_ recognizer: UITapGestureRecognizer) {
        resetZoom()
    }

    private func handleZoomedPan(_ recognizer: UIPanGestureRecognizer) {
        guard let plotArea = currentPlotArea else { return }
        switch recognizer.state {
        case .began:
            hideTouchMarker()
            updateClipMask(gestureActive: true)
        case .changed:
            applyLiveTransform(
                scaleX: 1, translationX: recognizer.translation(in: self).x, anchorX: 0)
        case .ended, .cancelled:
            let fraction = Double(recognizer.translation(in: self).x / plotArea.rect.width)
            zoomState?.pan(byFraction: fraction)
            commitViewport()
        default:
            break
        }
    }

    /// 확대 상태 팬은 가로 우세 움직임만 시작 (세로는 바깥 스크롤뷰로)
    static func isHorizontalDominant(translation: CGPoint) -> Bool {
        abs(translation.x) > abs(translation.y)
    }

    private func installGestures() {
        let pan = UIPanGestureRecognizer(target: self, action: #selector(handleGesture(_:)))
        pan.delegate = self
        addGestureRecognizer(pan)

        markerTapRecognizer.addTarget(self, action: #selector(handleGesture(_:)))
        markerTapRecognizer.delegate = self
        addGestureRecognizer(markerTapRecognizer)

        pinchRecognizer.addTarget(self, action: #selector(handlePinch(_:)))
        pinchRecognizer.delegate = self
        pinchRecognizer.isEnabled = isZoomEnabled
        addGestureRecognizer(pinchRecognizer)

        // 더블탭은 여기서 설정만 하고 부착하지 않는다 — isZoomEnabled didSet이 부착/해제.
        // requirement는 타깃이 뷰에 미부착이면 효력 없으므로 한 번만 걸어 둔다.
        doubleTapRecognizer.numberOfTapsRequired = 2
        doubleTapRecognizer.addTarget(self, action: #selector(handleDoubleTap(_:)))
        doubleTapRecognizer.delegate = self
        markerTapRecognizer.require(toFail: doubleTapRecognizer)  // 단일 탭(마커)은 더블탭 실패 후 발동
    }

    @objc private func handleGesture(_ recognizer: UIGestureRecognizer) {
        if let pan = recognizer as? UIPanGestureRecognizer, zoomState?.isZoomed == true {
            handleZoomedPan(pan)
            return
        }
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

// MARK: - UIGestureRecognizerDelegate
extension RDChartView: UIGestureRecognizerDelegate {
    /// 스크롤 컨테이너(UIScrollView) 안에서도 터치 마커가 동작하도록 동시 인식을 허용한다.
    /// 세로 스크롤 중에도 마커가 따라오는 감각은 DGCharts drag-highlight와 동일.
    /// 확대 상태의 팬은 차트가 독점 (스크롤뷰로 새지 않게). 그 외엔 기존대로 동시 인식.
    public func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        if zoomState?.isZoomed == true, gestureRecognizer is UIPanGestureRecognizer {
            return false
        }
        return true
    }

    public override func gestureRecognizerShouldBegin(
        _ gestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        if let pan = gestureRecognizer as? UIPanGestureRecognizer,
           zoomState?.isZoomed == true {
            return Self.isHorizontalDominant(translation: pan.translation(in: self))
        }
        return super.gestureRecognizerShouldBegin(gestureRecognizer)
    }
}
