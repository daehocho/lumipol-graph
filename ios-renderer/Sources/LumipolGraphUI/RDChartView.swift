import UIKit
import LumipolGraph

/// 스크럽(터치 근접점) 값을 소비자에게 전달하는 델리게이트.
public protocol RDChartScrubDelegate: AnyObject {
    /// 스크럽 근접점 값. seriesId → 포맷된 값 문자열(labelFormatter 결과).
    func chartView(_ view: RDChartView, didScrubTo valuesBySeriesId: [String: String])
    /// 스크럽 종료(손 뗌·줌/팬 진입 등으로 마커가 사라질 때).
    func chartViewDidEndScrub(_ view: RDChartView)
    /// 스크럽 근접점의 배경 area(예: 고도) 실값. backgroundArea가 있을 때만 호출.
    func chartView(_ view: RDChartView, didScrubToBackgroundValue value: Double)
}

public extension RDChartScrubDelegate {
    func chartView(_ view: RDChartView, didScrubToBackgroundValue value: Double) {}
}

/// KMP 코어 `LineChartData`를 받아 CAShapeLayer로 라인차트를 그리는 UIView.
/// 파이프라인: `LineChartEngine.layout` → `PlotArea`(픽셀 변환) → `ChartLayerBuilder`(레이어 조립).
@objc(RDChartView)
public final class RDChartView: UIView {

    /// main 라인 등장 애니메이션(strokeEnd 0→1). 스냅샷/테스트에서는 끈다.
    @objc public var isAnimationEnabled: Bool = true

    /// 스크럽 값 소비자. 미설정 시 값 전달 없음(기존 동작).
    public weak var scrubDelegate: RDChartScrubDelegate?

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
    private let longPressRecognizer = UILongPressGestureRecognizer()

    public override init(frame: CGRect) {
        super.init(frame: frame)
        installGestures()
    }

    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        installGestures()
    }

    private(set) var data: LineChartData?
    private(set) var chartLayout: LineChartLayout? {
        didSet { cachedXAxisScale = nil }
    }
    /// 현재 chartLayout의 X 스케일 캐시 — 스크럽(60~120Hz)마다 tick 탐색·재구성을 피한다.
    private var cachedXAxisScale: AxisScale?
    private(set) var style: ChartStyle = .default
    private(set) var invertedAxes: Set<Axis> = []
    private(set) var labelFormatter: (ChartAxis, Double) -> String = RDChartView.defaultFormatter
    private(set) var currentPlotArea: PlotArea?
    private var chartLayers: [CALayer] = []
    // 줌 변환 대상 콘텐츠(시리즈·그리드·밴드·마커·기준선)와 클립 컨테이너.
    // 축 라벨·터치 마커는 루트에 남아 변환/클립을 받지 않는다.
    private let clipContainer = CALayer()
    private let contentContainer = CALayer()
    private var touchMarkerLayer: CALayer?
    private var activeMarkerRawX: Double?
    private var needsEntranceAnimation = false
    /// 확대 팬 진행 중 기준 창(제스처 시작 시점). 매 프레임 이 창에 누적 이동량을 적용해 재렌더.
    private var panStartWindow: ClosedRange<Double>?
    /// 라이브 핀치 진행 중 기준 창(제스처 시작 시점). 매 프레임 누적 배율을 적용해 재렌더.
    private var pinchStartWindow: ClosedRange<Double>?
    /// 롱프레스 스크럽 모드 여부. 켜져 있으면 팬(스크롤)을 잠근다.
    private var isScrubbing = false
    /// 페이스 라인 뒤 배경 고도 실루엣(장식). nil이면 그리지 않음.
    private var backgroundArea: [AreaPoint]?
    /// backgroundArea의 코어 타입 변환본(스크럽 보간 질의용) — 스크럽은 60~120Hz로 호출되므로
    /// 매번 재변환하지 않고 render 시 1회 생성한다.
    private var backgroundAreaPoints: [Point]?
    /// 마지막 rebuildLayers 입력 식별자. bounds와 chartLayout 인스턴스가 그대로면
    /// 레이아웃 패스가 반복돼도 레이어 트리를 재구축하지 않는다(경로 재계산·라벨 재측정 방지).
    /// 스타일·반전·backgroundArea는 render()를 통해서만 바뀌고 그때마다 새 layout이 생성되므로 키에 불필요.
    private struct RebuildKey: Equatable {
        let bounds: CGRect
        let layout: ObjectIdentifier
    }
    private var lastRebuildKey: RebuildKey?

    /// 차트를 그린다. 터치 질의를 위해 `data`를 보관한다.
    ///
    /// 레이어는 즉시 만들어지지 않고 **다음 레이아웃 패스(`layoutSubviews`)에서 구축**된다.
    /// `render()` 직후 레이어가 필요하면(스냅샷 등) `layoutIfNeeded()`를 호출할 것.
    /// - Parameters:
    ///   - invertedAxes: 화면에서 뒤집을 Y축(예: 페이스 — 위=빠름). 코어 출력은 값-공간 그대로.
    ///   - labelFormatter: 축 tick 값 → 표시 문자열. 코어/렌더러는 단위를 모른다(앱 주입).
    ///     주의: 오버레이 시리즈가 있으면 스크럽 시 `ChartAxis.yOverlay`(실값)로도 호출된다 —
    ///     축 keyed 딕셔너리·강제 언래핑 포매터는 이 케이스를 반드시 처리할 것.
    public func render(
        _ data: LineChartData,
        style: ChartStyle = .default,
        invertedAxes: Set<Axis> = [],
        labelFormatter: ((ChartAxis, Double) -> String)? = nil,
        backgroundArea: [AreaPoint]? = nil
    ) {
        removeTouchMarkerLayer()
        zoomState = nil
        self.data = data
        // 코어 interpolatedY의 이진 탐색·클램프는 x 오름차순을 전제한다 — 호출자 순서에 기대지 않고
        // 저장 시점에 정규화한다(실루엣 렌더는 순서 무관이라 비정렬 입력이 시각적으로 드러나지 않음).
        self.backgroundArea = backgroundArea?.sorted { $0.x < $1.x }
        self.backgroundAreaPoints = self.backgroundArea?.map { Point(x: $0.x, y: $0.y) }
        self.style = style
        self.invertedAxes = invertedAxes
        self.labelFormatter = labelFormatter ?? RDChartView.defaultFormatter
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
        // 입력(bounds·layout)이 그대로면 조기 반환 — 상위 레이아웃 패스마다 트리를 재구축하지 않는다.
        if let chartLayout,
           lastRebuildKey == RebuildKey(bounds: bounds, layout: ObjectIdentifier(chartLayout)) {
            return
        }
        lastRebuildKey = nil

        CATransaction.begin()
        CATransaction.setDisableActions(true)
        defer { CATransaction.commit() }

        let markerRawX = activeMarkerRawX
        removeTouchMarkerLayer()
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
        if let backgroundArea, let xScale = xAxisScale(),
           let areaLayer = AreaSilhouette.layer(
               points: backgroundArea, xScale: xScale, plotArea: plotArea, style: style
           ) {
            contentContainer.addSublayer(areaLayer)  // 최하단(그리드보다 아래)
        }
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
            showTouchMarker(atX: markerRawX, notifyingDelegate: false)
        }
        updateClipMask()
        lastRebuildKey = RebuildKey(bounds: bounds, layout: ObjectIdentifier(chartLayout))
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

    /// 원본 도메인 x 위치에 근접점 마커(수직선+점)를 표시하고 값을 델리게이트로 전달한다.
    @objc public func showTouchMarker(atX rawX: Double) {
        showTouchMarker(atX: rawX, notifyingDelegate: true)
    }

    /// 마커 표시 공통 경로. `notifyingDelegate: false`는 레이아웃 패스의 마커 복원 전용 —
    /// 사용자 입력이 없으므로 스크럽 콜백(햅틱/애널리틱스 부작용)을 재발화하지 않는다.
    private func showTouchMarker(atX rawX: Double, notifyingDelegate: Bool) {
        var rawX = rawX
        if let state = zoomState, state.isZoomed {
            // 창 끝 스크럽은 도메인 역산 반올림으로 상/하한을 수 ulp 벗어날 수 있다 —
            // TouchMarker의 경계 처리와 동일하게 epsilon 이내는 창 안으로 클램프한다
            // (엄격 비교 시 끝 스크럽이 침묵 드롭되고 이전 마커가 얼어붙음).
            let epsilon = (state.window.upperBound - state.window.lowerBound) * 1e-9
            guard rawX >= state.window.lowerBound - epsilon,
                  rawX <= state.window.upperBound + epsilon else { return }
            rawX = min(max(rawX, state.window.lowerBound), state.window.upperBound)
        }
        guard let data, let chartLayout, let plotArea = currentPlotArea else { return }
        let hadMarker = touchMarkerLayer != nil
        removeTouchMarkerLayer()
        let context = TouchMarker.Context(
            data: data, layout: chartLayout, style: style,
            plotArea: plotArea, formatter: labelFormatter
        )
        guard let result = TouchMarker.make(atRawX: rawX, context: context) else {
            // 표시 중이던 마커가 사라질 때만 종료 통지 — 마커가 없었으면
            // didScrubTo 없는 endScrub(짝 깨진 콜백)를 만들지 않는다 (hideTouchMarker와 동일 계약).
            if notifyingDelegate, hadMarker { scrubDelegate?.chartViewDidEndScrub(self) }
            return
        }
        layer.addSublayer(result.layer)
        touchMarkerLayer = result.layer
        activeMarkerRawX = rawX
        guard notifyingDelegate else { return }
        scrubDelegate?.chartView(self, didScrubTo: result.valuesBySeriesId)
        // 배경 area 보간은 코어 질의(interpolatedY) — 양 플랫폼 렌더러가 동일 로직 공유 (0.9.0 이관).
        if let backgroundAreaPoints,
           let value = LineChartEngine.shared.interpolatedY(
               points: backgroundAreaPoints, x: result.snappedX)
        {
            scrubDelegate?.chartView(self, didScrubToBackgroundValue: value.doubleValue)
        }
    }

    /// 마커 레이어만 제거(델리게이트 통지 없음) — 내부 재빌드·재렌더용.
    private func removeTouchMarkerLayer() {
        touchMarkerLayer?.removeFromSuperlayer()
        touchMarkerLayer = nil
        activeMarkerRawX = nil
    }

    /// 스크럽 종료: 마커 제거 + (표시 중이었으면) 종료 통지.
    @objc public func hideTouchMarker() {
        let hadMarker = touchMarkerLayer != nil
        removeTouchMarkerLayer()
        if hadMarker { scrubDelegate?.chartViewDidEndScrub(self) }
    }

    // MARK: - Zoom

    /// 프로그래매틱 줌. isZoomEnabled가 꺼져 있으면 무시.
    public func zoom(toXRange range: ClosedRange<Double>) {
        guard isZoomEnabled, range.upperBound > range.lowerBound else { return }
        ensureZoomState()
        zoomState?.setWindow(range)
        hideTouchMarker()
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
        // zoomState==nil ⇒ 현재 layout이 전체 구간 (불변식) — xAxisScale()이 곧 전체 도메인 스케일.
        guard zoomState == nil, let scale = xAxisScale() else { return }
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
            // 1x 복귀 시 상태 제거 (ensureZoomState 불변식 유지).
            // 단, 라이브 제스처 진행 중에는 유지 — 창이 잠깐 전체 구간을 지나가며
            // isZoomed=false가 되어도 같은 제스처로 다시 확대할 수 있어야 한다.
            if pinchStartWindow == nil, panStartWindow == nil {
                zoomState = nil
            }
            chartLayout = LineChartEngine.shared.layout(data: data)
        }
        needsEntranceAnimation = false
        setNeedsLayout()
        layoutIfNeeded()
        updateClipMask()
    }

    /// 확대(줌 창) 상태일 때만 플롯 영역 클립 — 창 밖으로 이어지는 이웃 포인트 선을 가린다.
    /// 마스크 위쪽을 뷰 상단까지 열어 구간(km) 마커 라벨은 잘리지 않게 한다.
    private func updateClipMask() {
        guard let plotArea = currentPlotArea else { return }
        if zoomState?.isZoomed == true {
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

    /// 라이브 핀치. 콘텐츠 변환(스케일)은 현재 창 조각만 갖고 있어 확대 시 잘리거나 축소 시 빈 공간이
    /// 노출된다 — 대신 매 프레임 창을 재계산해 재렌더(팬과 동일 전략). 앵커(손가락 중점)를 고정점으로 유지.
    @objc private func handlePinch(_ recognizer: UIPinchGestureRecognizer) {
        guard isZoomEnabled, let plotArea = currentPlotArea, data != nil else { return }
        switch recognizer.state {
        case .began:
            pinchBegan()
        case .changed:
            let anchor = plotArea.normalizedX(at: recognizer.location(in: self).x)
            pinchChanged(cumulativeScale: Double(recognizer.scale), anchor: anchor)
        case .ended, .cancelled:
            pinchEnded()
        default:
            break
        }
    }

    // 핀치 제스처 단계별 시임 — recognizer 없이 단위 테스트 가능하도록 분리.
    func pinchBegan() {
        ensureZoomState()
        hideTouchMarker()
        pinchStartWindow = zoomState?.window
    }

    func pinchChanged(cumulativeScale: Double, anchor: Double) {
        guard let start = pinchStartWindow else { return }
        zoomState?.pinch(
            from: start, cumulativeScale: cumulativeScale,
            anchor: anchor, maxScale: Double(maxZoomScale)
        )
        commitViewport()
    }

    func pinchEnded() {
        pinchStartWindow = nil
        // 제스처가 1x에서 끝났으면 지연된 상태 정리 수행 (commitViewport가 제스처 중엔 유지).
        if zoomState?.isZoomed != true { zoomState = nil }
    }

    @objc private func handleDoubleTap(_ recognizer: UITapGestureRecognizer) {
        resetZoom()
    }

    /// 확대 상태 팬. 라이브 변환(콘텐츠 밀기)은 현재 창 조각만 갖고 있어 인접 구간이 빈 공간으로
    /// 노출된다 — 대신 매 프레임 창을 이동시켜 재렌더(코어 재계산)한다. 항상 꽉 차고 Y도 실시간 재계산된다.
    /// 기준 창(제스처 시작 시점)에 누적 이동량을 적용해 드리프트를 막는다.
    private func handleZoomedPan(_ recognizer: UIPanGestureRecognizer) {
        if pinchRecognizer.state == .began || pinchRecognizer.state == .changed { return }
        guard let plotArea = currentPlotArea, plotArea.rect.width > 0 else { return }
        switch recognizer.state {
        case .began:
            hideTouchMarker()
            panStartWindow = zoomState?.window
        case .changed:
            guard let start = panStartWindow else { return }
            let span = start.upperBound - start.lowerBound
            // 오른쪽 드래그(+x) = 이전(왼쪽) 구간으로 → 창 왼쪽 이동.
            let fraction = Double(recognizer.translation(in: self).x / plotArea.rect.width)
            let targetLower = start.lowerBound - fraction * span
            zoomState?.setWindow(targetLower ... (targetLower + span))
            commitViewport()
        case .ended, .cancelled:
            panStartWindow = nil
            // 핀치와 동일 — 팬이 전체 구간에서 끝났으면 지연된 상태 정리.
            if zoomState?.isZoomed != true { zoomState = nil }
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
        pan.maximumNumberOfTouches = 1
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

        longPressRecognizer.minimumPressDuration = 0.5
        longPressRecognizer.addTarget(self, action: #selector(handleLongPress(_:)))
        longPressRecognizer.delegate = self
        addGestureRecognizer(longPressRecognizer)
    }

    @objc private func handleGesture(_ recognizer: UIGestureRecognizer) {
        // 롱프레스 스크럽 중에는 팬(스크롤)을 무시해 확대 창을 고정한다.
        if recognizer is UIPanGestureRecognizer, isScrubbing { return }
        // 진행 중인 확대 팬은 창이 전체로 돌아가(isZoomed=false) 되어도 제스처 끝까지 팬 핸들러 유지.
        if let pan = recognizer as? UIPanGestureRecognizer,
           zoomState?.isZoomed == true || panStartWindow != nil {
            handleZoomedPan(pan)
            return
        }
        if pinchRecognizer.state == .began || pinchRecognizer.state == .changed { return }
        // 손을 떼면(팬 종료) 마커 제거 + 값 원복. 탭(markerTap)은 종료 분기에 안 걸려 기존대로 표시 유지.
        if recognizer is UIPanGestureRecognizer,
           recognizer.state == .ended || recognizer.state == .cancelled || recognizer.state == .failed {
            hideTouchMarker()
            return
        }
        scrub(at: recognizer.location(in: self))
    }

    /// 확대 상태에서 0.5초 롱프레스 후 드래그 = 스크럽(값 조회). 진입 시 햅틱, 스크럽 중 팬 잠금.
    /// 100%에서도 발동하지만 기존 드래그 스크럽과 결과 동일해 무해.
    @objc private func handleLongPress(_ recognizer: UILongPressGestureRecognizer) {
        switch recognizer.state {
        case .began:
            isScrubbing = true
            // 햅틱은 "확대 상태 값 조회 진입" 신호 — 100%(비확대)에선 기존 드래그 스크럽과 동일해 울리지 않는다.
            if zoomState?.isZoomed == true {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            }
            scrub(at: recognizer.location(in: self))
        case .changed:
            scrub(at: recognizer.location(in: self))
        case .ended, .cancelled, .failed:
            isScrubbing = false
            hideTouchMarker()
        default:
            break
        }
    }

    /// 현재 chartLayout의 X tick으로 도메인↔정규화 변환 스케일. tick 부족 시 nil.
    /// chartLayout 교체 시까지 캐시된다.
    private func xAxisScale() -> AxisScale? {
        if let cachedXAxisScale { return cachedXAxisScale }
        guard let xTicks = chartLayout?.axisTicks.first(where: { $0.axis == .x })?.ticks else {
            return nil
        }
        cachedXAxisScale = AxisScale(ticks: xTicks)
        return cachedXAxisScale
    }

    /// 손가락 뷰 좌표 → 현재(창) 도메인 x로 환산해 스크럽 마커 표시. 롱프레스·비확대 스크럽 공용.
    func scrub(at location: CGPoint) {
        guard let plotArea = currentPlotArea, let xScale = xAxisScale() else { return }
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
