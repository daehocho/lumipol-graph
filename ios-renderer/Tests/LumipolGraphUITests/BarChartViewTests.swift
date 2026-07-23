import XCTest
@testable import LumipolGraphUI

final class BarChartViewTests: XCTestCase {

    private func sampleLayout(barCount: Int, refPos: Double? = 0.5) -> BarChartLayout {
        let bars = (0..<barCount).map { i in
            BarLayout(
                index: Int32(i),
                value: 300.0,
                heightFraction: 0.3 + 0.1 * Double(i),
                colorRole: .onTarget,
                isPartial: i == barCount - 1,
                endMinutes: nil
            )
        }
        let ticks = [AxisTick(value: 300, position: 0.2), AxisTick(value: 360, position: 0.8)]
        return BarChartLayout(bars: bars, yTicks: ticks, referenceLinePosition: refPos.map { KotlinDouble(double: $0) })
    }

    func testRendersOneLayerPerBar() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(sampleLayout(barCount: 4))
        XCTAssertEqual(view.barLayers.count, 4)
    }

    func testEmptyLayoutRendersNoBars() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(BarChartLayout(bars: [], yTicks: [], referenceLinePosition: nil))
        XCTAssertEqual(view.barLayers.count, 0)
    }

    func testReRenderClearsPreviousBars() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(sampleLayout(barCount: 4))
        view.render(sampleLayout(barCount: 2))
        XCTAssertEqual(view.barLayers.count, 2)
    }

    // 값이 다른 3막대(페이스 300/330/360) 레이아웃 — 연속 색상 검증용.
    private func pacedLayout() -> BarChartLayout {
        let bars = [
            BarLayout(index: 0, value: 300, heightFraction: 0.3, colorRole: .faster, isPartial: false, endMinutes: nil),
            BarLayout(index: 1, value: 330, heightFraction: 0.5, colorRole: .onTarget, isPartial: false, endMinutes: nil),
            BarLayout(index: 2, value: 360, heightFraction: 0.7, colorRole: .slower, isPartial: false, endMinutes: nil),
        ]
        return BarChartLayout(bars: bars, yTicks: [], referenceLinePosition: nil)
    }

    private func rgbaOf(_ cg: CGColor?) -> (CGFloat, CGFloat, CGFloat) {
        let c = UIColor(cgColor: cg!)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        c.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (r, g, b)
    }

    // provider 미지정 → 기본 연속 팔레트. fastest(300)=파랑끝 RGB(0,0.6,1).
    func testDefaultContinuousColorApplied() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(pacedLayout())
        let (r, g, b) = rgbaOf(view.barLayers[0].backgroundColor)
        XCTAssertEqual(r, 0, accuracy: 0.01)
        XCTAssertEqual(g, 0.6, accuracy: 0.01)
        XCTAssertEqual(b, 1, accuracy: 0.01)
    }

    // provider 지정 → 그 색 사용.
    func testProviderOverridesColor() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var style = ChartStyle.default
        style.barColorProvider = { _ in .magenta }
        view.render(pacedLayout(), style: style)
        for layer in view.barLayers {
            XCTAssertEqual(layer.backgroundColor, UIColor.magenta.cgColor)
        }
    }

    // provider가 받는 앵커(fastest/slowest/average/index/value)가 정확한지.
    func testProviderReceivesCorrectAnchors() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var captured: [BarPaceColorInput] = []
        var style = ChartStyle.default
        style.barColorProvider = { input in captured.append(input); return .black }
        view.render(pacedLayout(), style: style)
        XCTAssertEqual(captured.count, 3)
        XCTAssertEqual(captured.map { $0.value }, [300, 330, 360])
        XCTAssertEqual(captured.map { $0.index }, [0, 1, 2])
        for c in captured {
            XCTAssertEqual(c.fastest, 300, accuracy: 0.001)
            XCTAssertEqual(c.slowest, 360, accuracy: 0.001)
            XCTAssertEqual(c.average, 330, accuracy: 0.001)  // (300+330+360)/3
        }
    }

    // 부분 스플릿(극단값)은 색 앵커에서 제외 — 온전한 스플릿만으로 계산.
    func testAnchorsExcludePartialSplit() {
        let bars = [
            BarLayout(index: 0, value: 300, heightFraction: 0.4, colorRole: .faster, isPartial: false, endMinutes: nil),
            BarLayout(index: 1, value: 330, heightFraction: 0.5, colorRole: .onTarget, isPartial: false, endMinutes: nil),
            BarLayout(index: 2, value: 360, heightFraction: 0.6, colorRole: .slower, isPartial: false, endMinutes: nil),
            BarLayout(index: 3, value: 100, heightFraction: 0.1, colorRole: .faster, isPartial: true, endMinutes: nil), // 극단 outlier
        ]
        let layout = BarChartLayout(bars: bars, yTicks: [], referenceLinePosition: nil)
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var captured: [BarPaceColorInput] = []
        var style = ChartStyle.default
        style.barColorProvider = { input in captured.append(input); return .black }
        view.render(layout, style: style)
        XCTAssertEqual(captured.count, 4)
        for c in captured {                       // 앵커는 온전한 스플릿(300/330/360)만
            XCTAssertEqual(c.fastest, 300, accuracy: 0.001)
            XCTAssertEqual(c.slowest, 360, accuracy: 0.001)
            XCTAssertEqual(c.average, 330, accuracy: 0.001)  // (300+330+360)/3, 부분 100 제외
        }
        XCTAssertEqual(captured[3].value, 100, accuracy: 0.001)   // 부분 막대 자신은 여전히 색칠됨
        XCTAssertTrue(captured[3].isPartial)
    }

    // 전부 부분 스플릿이면 전체로 폴백(크래시 없음).
    func testAnchorsFallBackWhenAllPartial() {
        let bars = [BarLayout(index: 0, value: 250, heightFraction: 0.5, colorRole: .onTarget, isPartial: true, endMinutes: nil)]
        let layout = BarChartLayout(bars: bars, yTicks: [], referenceLinePosition: nil)
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var captured: [BarPaceColorInput] = []
        var style = ChartStyle.default
        style.barColorProvider = { input in captured.append(input); return .black }
        view.render(layout, style: style)
        XCTAssertEqual(captured.count, 1)
        XCTAssertEqual(captured[0].average, 250, accuracy: 0.001)  // 폴백: 그 막대 자신
    }

    // 짧은 런(온전 스플릿 1 + 부분 1): 온전 스플릿만으론 범위가 없어 전체로 폴백 → 색 신호 보존.
    func testFallsBackToAllWhenFullSplitsLackRange() {
        let bars = [
            BarLayout(index: 0, value: 300, heightFraction: 0.6, colorRole: .faster, isPartial: false, endMinutes: nil),
            BarLayout(index: 1, value: 360, heightFraction: 0.3, colorRole: .slower, isPartial: true, endMinutes: nil),
        ]
        let layout = BarChartLayout(bars: bars, yTicks: [], referenceLinePosition: nil)
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var captured: [BarPaceColorInput] = []
        var style = ChartStyle.default
        style.barColorProvider = { input in captured.append(input); return .black }
        view.render(layout, style: style)
        XCTAssertEqual(captured.count, 2)
        XCTAssertEqual(captured[0].fastest, 300, accuracy: 0.001)   // 폴백: 부분 포함 전체
        XCTAssertEqual(captured[0].slowest, 360, accuracy: 0.001)
        XCTAssertGreaterThan(captured[0].slowest, captured[0].fastest)  // 축퇴 아님
    }

    // 부분막대 흐림(opacity)은 색과 독립적으로 유지.
    func testPartialBarStillDimmedWithContinuousColor() {
        let bars = [
            BarLayout(index: 0, value: 300, heightFraction: 0.5, colorRole: .faster, isPartial: false, endMinutes: nil),
            BarLayout(index: 1, value: 360, heightFraction: 0.3, colorRole: .slower, isPartial: true, endMinutes: nil),
        ]
        let layout = BarChartLayout(bars: bars, yTicks: [], referenceLinePosition: nil)
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(layout)
        XCTAssertEqual(view.barLayers[0].opacity, 1.0, accuracy: 0.001)
        XCTAssertEqual(view.barLayers[1].opacity, 0.6, accuracy: 0.001)   // 부분막대
    }

    /// contentLayer 안의 CATextLayer 개수. barLabels:nil로 렌더하면 y틱 라벨만 남는다.
    private func textLayerCount(_ view: RDBarChartView) -> Int {
        (view.layer.sublayers ?? [])
            .flatMap { $0.sublayers ?? [] }
            .filter { $0 is CATextLayer }
            .count
    }

    func testYAxisLabelsHiddenWhenFlagFalse() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var style = ChartStyle.default
        style.barShowYAxisLabels = false
        view.render(sampleLayout(barCount: 3), style: style, barLabels: nil)
        XCTAssertEqual(textLayerCount(view), 0)   // y틱 라벨 없음
    }

    func testYAxisLabelsShownWhenFlagTrue() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var style = ChartStyle.default
        style.barShowYAxisLabels = true
        view.render(sampleLayout(barCount: 3), style: style, barLabels: nil)
        // sampleLayout는 yTicks 2개 → 라벨 2개
        XCTAssertEqual(textLayerCount(view), 2)
    }

    func testYLabelFormatterIsApplied() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 300, height: 200))
        let layout = BarChartLayout(
            bars: [BarLayout(index: 0, value: 300, heightFraction: 0.5, colorRole: .onTarget, isPartial: false, endMinutes: nil)],
            yTicks: [AxisTick(value: 300, position: 0.5)],
            referenceLinePosition: nil
        )
        var style = ChartStyle.default
        style.barShowYAxisLabels = true
        view.render(layout, style: style, barLabels: nil, xAxisLabels: nil,
                    yLabelFormatter: { value in "P\(Int(value))" })
        view.layoutIfNeeded()
        XCTAssertTrue(view.allTextLayerStrings.contains("P300"))
    }

    func testXAxisLabelsDrawnUnderBars() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 300, height: 200))
        let layout = BarChartLayout(
            bars: [
                BarLayout(index: 0, value: 300, heightFraction: 0.5, colorRole: .onTarget, isPartial: false, endMinutes: nil),
                BarLayout(index: 1, value: 310, heightFraction: 0.6, colorRole: .slower, isPartial: false, endMinutes: nil),
            ],
            yTicks: [], referenceLinePosition: nil
        )
        view.render(layout, style: .default, barLabels: nil, xAxisLabels: ["1", "2"], yLabelFormatter: nil)
        view.layoutIfNeeded()
        let strings = view.allTextLayerStrings
        XCTAssertTrue(strings.contains("1"))
        XCTAssertTrue(strings.contains("2"))
    }

    // 정적 막대 라벨 제거: barLabels를 줘도 막대 위 텍스트는 그리지 않는다(값은 롱프레스 말풍선으로).
    func testBarLabelsNotDrawnStatically() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 300, height: 200))
        var style = ChartStyle.default
        style.barShowYAxisLabels = false
        view.render(sampleLayout(barCount: 3), style: style,
                    barLabels: ["4'50\"", "5'00\"", "5'10\""], xAxisLabels: nil, yLabelFormatter: nil)
        view.layoutIfNeeded()
        XCTAssertTrue(view.allTextLayerStrings.isEmpty, "막대 위 정적 라벨이 없어야 함")
    }

    func testXAxisLabelsThinnedWhenTooNarrow() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
        let x = Array(repeating: "5'30\"", count: 20)
        view.render(sampleLayout(barCount: 20), style: .default, barLabels: nil,
                    xAxisLabels: x, yLabelFormatter: nil)
        view.layoutIfNeeded()
        let shown = view.allTextLayerStrings.filter { $0 == "5'30\"" }.count
        XCTAssertLessThan(shown, 20)
        XCTAssertGreaterThan(shown, 0)
    }

    func testXAxisLabelsHiddenWhenFlagOff() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 300, height: 200))
        let layout = BarChartLayout(
            bars: [BarLayout(index: 0, value: 300, heightFraction: 0.5, colorRole: .onTarget, isPartial: false, endMinutes: nil)],
            yTicks: [], referenceLinePosition: nil
        )
        var style = ChartStyle.default
        style.barShowXAxisLabels = false
        view.render(layout, style: style, barLabels: nil, xAxisLabels: ["1"], yLabelFormatter: nil)
        view.layoutIfNeeded()
        XCTAssertFalse(view.allTextLayerStrings.contains("1"))
    }

    func testDimsUnselectedBars() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(sampleLayout(barCount: 4))   // barLabels 없이도 dim은 동작
        view.selectBar(at: 1)
        XCTAssertEqual(view.selectedIndex, 1)
        XCTAssertEqual(view.barLayers[1].opacity, 1.0, accuracy: 0.001)      // 선택 막대 = 기본
        XCTAssertLessThan(view.barLayers[0].opacity, 1.0)                    // 미선택 dim
        XCTAssertEqual(view.barLayers[0].opacity, Float(0.35), accuracy: 0.001)
        // 부분 막대(마지막)는 기본 0.6 → dim 시 0.6*0.35
        XCTAssertEqual(view.barLayers[3].opacity, Float(0.6 * 0.35), accuracy: 0.001)
    }

    func testDeselectRestoresOpacity() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(sampleLayout(barCount: 4))
        view.selectBar(at: 1)
        view.selectBar(at: nil)
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(view.barLayers[0].opacity, 1.0, accuracy: 0.001)
        XCTAssertEqual(view.barLayers[3].opacity, 0.6, accuracy: 0.001)     // 부분 막대 원복
    }

    func testBarIndexAtXMapsSlotsAndClamps() {
        // plotMinX=0, plotWidth=100, count=5 → slot=20
        XCTAssertEqual(RDBarChartView.barIndex(atX: 10, plotMinX: 0, plotWidth: 100, count: 5), 0)
        XCTAssertEqual(RDBarChartView.barIndex(atX: 25, plotMinX: 0, plotWidth: 100, count: 5), 1)
        XCTAssertEqual(RDBarChartView.barIndex(atX: 99, plotMinX: 0, plotWidth: 100, count: 5), 4)
        // 경계 밖 → 클램프
        XCTAssertEqual(RDBarChartView.barIndex(atX: -50, plotMinX: 0, plotWidth: 100, count: 5), 0)
        XCTAssertEqual(RDBarChartView.barIndex(atX: 500, plotMinX: 0, plotWidth: 100, count: 5), 4)
        // plotMinX 오프셋
        XCTAssertEqual(RDBarChartView.barIndex(atX: 45, plotMinX: 40, plotWidth: 100, count: 5), 0)
        // 축퇴 입력
        XCTAssertNil(RDBarChartView.barIndex(atX: 10, plotMinX: 0, plotWidth: 100, count: 0))
        XCTAssertNil(RDBarChartView.barIndex(atX: 10, plotMinX: 0, plotWidth: 0, count: 5))
        // count==1
        XCTAssertEqual(RDBarChartView.barIndex(atX: 999, plotMinX: 0, plotWidth: 100, count: 1), 0)
    }

    // 오버레이 안의 가이드선(CAShapeLayer, name=bar.selection.line) 존재 여부.
    private func hasSelectionGuide(_ view: RDBarChartView) -> Bool {
        (view.layer.sublayers ?? [])
            .flatMap { $0.sublayers ?? [] }
            .contains { $0.name == "bar.selection.line" }
    }

    func testSelectionShowsCalloutNoGuide() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var style = ChartStyle.default
        style.barShowYAxisLabels = false
        view.render(sampleLayout(barCount: 4), style: style,
                    barLabels: ["4'50\"", "5'00\"", "5'10\"", "5'20\""], xAxisLabels: nil, yLabelFormatter: nil)
        view.selectBar(at: 2)
        XCTAssertFalse(hasSelectionGuide(view), "선택 시 세로 가이드선은 그리지 않는다")
        XCTAssertTrue(view.allTextLayerStrings.contains("5'10\""), "말풍선 페이스 = barLabels[2]")
    }

    func testDeselectRemovesOverlay() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var style = ChartStyle.default
        style.barShowYAxisLabels = false
        view.render(sampleLayout(barCount: 4), style: style,
                    barLabels: ["4'50\"", "5'00\"", "5'10\"", "5'20\""], xAxisLabels: nil, yLabelFormatter: nil)
        view.selectBar(at: 2)
        view.selectBar(at: nil)
        XCTAssertFalse(hasSelectionGuide(view))
        XCTAssertTrue(view.allTextLayerStrings.isEmpty, "말풍선 텍스트 제거")
    }

    // 말풍선이 플롯 좌우 경계를 넘지 않도록 클램프(끝 막대 선택).
    func testCalloutClampedWithinPlot() throws {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var style = ChartStyle.default
        style.barShowYAxisLabels = false
        view.render(sampleLayout(barCount: 4), style: style,
                    barLabels: ["4'50\"", "5'00\"", "5'10\"", "5'20\""], xAxisLabels: nil, yLabelFormatter: nil)
        view.selectBar(at: 3)   // 맨 오른쪽
        let plot = view.bounds.inset(by: style.plotInsets)
        let bubble = (view.layer.sublayers ?? [])
            .flatMap { $0.sublayers ?? [] }
            .first { $0.name == "bar.selection.bubble" }
        let f = try XCTUnwrap(bubble).frame
        XCTAssertLessThanOrEqual(f.maxX, plot.maxX + 0.5)
        XCTAssertGreaterThanOrEqual(f.minX, plot.minX - 0.5)
    }

    func testScrubSelectsBarUnderFinger() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var style = ChartStyle.default
        style.barShowYAxisLabels = false
        view.render(sampleLayout(barCount: 4), style: style,
                    barLabels: ["4'50\"", "5'00\"", "5'10\"", "5'20\""], xAxisLabels: nil, yLabelFormatter: nil)
        let plot = view.bounds.inset(by: style.plotInsets)
        let slot = plot.width / 4
        // 인덱스 2 슬롯 중앙
        let x = plot.minX + slot * 2 + slot / 2
        view.scrub(at: CGPoint(x: x, y: 100))
        XCTAssertEqual(view.selectedIndex, 2)
    }

    func testScrubIgnoredWithoutBarLabels() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(sampleLayout(barCount: 4))   // barLabels 없음
        view.scrub(at: CGPoint(x: 160, y: 100))
        XCTAssertNil(view.selectedIndex, "값 소스 없으면 선택 안 함")
    }

    func testScrubIgnoredWithEmptyBarLabels() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(sampleLayout(barCount: 4), style: .default, barLabels: [], xAxisLabels: nil, yLabelFormatter: nil)
        view.scrub(at: CGPoint(x: 160, y: 100))
        XCTAssertNil(view.selectedIndex, "barLabels가 빈 배열이면 선택 안 함")
    }

    // 재렌더 시 이전 선택 상태(인덱스/오버레이)가 초기화되는지 확인.
    func testSelectionClearedOnReRender() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(sampleLayout(barCount: 4), style: .default,
                    barLabels: ["4'50\"", "5'00\"", "5'10\"", "5'20\""], xAxisLabels: nil, yLabelFormatter: nil)
        view.selectBar(at: 2)
        view.render(sampleLayout(barCount: 2), style: .default,
                    barLabels: ["4'50\"", "5'00\""], xAxisLabels: nil, yLabelFormatter: nil)
        view.layoutIfNeeded()
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(view.barLayers[0].opacity, 1.0, accuracy: 0.001, "재렌더 후 막대가 dim 상태로 남아 있으면 안 됨")
        XCTAssertFalse(hasSelectionGuide(view), "재렌더 후 이전 선택 가이드선이 남아 있으면 안 됨")
    }

    func testLongPressAllowsSimultaneousRecognition() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        let longPress = try! XCTUnwrap(view.gestureRecognizers?.compactMap { $0 as? UILongPressGestureRecognizer }.first)
        XCTAssertTrue(longPress.delegate === view, "롱프레스 델리게이트가 뷰여야 함")
        XCTAssertTrue(view.gestureRecognizer(longPress, shouldRecognizeSimultaneouslyWith: UIPanGestureRecognizer()),
                      "스크롤 팬과 동시 인식 허용(공존)")
    }

    func testSelectionColorsResolveForCurrentTraits() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        // overrideUserInterfaceStyle은 뷰가 UIWindow 계층에 있어야 traitCollection에 실제로 반영된다
        // (윈도우 밖에서는 system 기본값에 머묾) — 다크 트레잇 해석을 검증하려면 윈도우에 호스팅 필요.
        let window = UIWindow(frame: view.frame)
        window.addSubview(view)
        window.makeKeyAndVisible()
        view.overrideUserInterfaceStyle = .dark
        var style = ChartStyle.default
        style.barShowYAxisLabels = false
        view.render(sampleLayout(barCount: 4), style: style,
                    barLabels: ["4'50\"", "5'00\"", "5'10\"", "5'20\""], xAxisLabels: nil, yLabelFormatter: nil)
        view.selectBar(at: 2)
        let bubble = try! XCTUnwrap((view.layer.sublayers ?? [])
            .flatMap { $0.sublayers ?? [] }
            .first { $0.name == "bar.selection.bubble" } as? CAShapeLayer)
        let darkResolved = style.barCalloutBackgroundColor.resolvedColor(with: view.traitCollection).cgColor
        let lightResolved = style.barCalloutBackgroundColor.resolvedColor(
            with: UITraitCollection(userInterfaceStyle: .light)).cgColor
        XCTAssertEqual(bubble.fillColor, darkResolved, "말풍선 배경색은 현재(다크) 트레잇으로 해석")
        XCTAssertNotEqual(darkResolved, lightResolved, "전제: label 색은 라이트/다크가 다름")
    }

    func testCalloutClampedToLeftWhenBubbleWiderThanPlot() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 120, height: 200)) // plot.width = 120-88 = 32
        var style = ChartStyle.default
        style.barShowYAxisLabels = false
        view.render(sampleLayout(barCount: 4), style: style,
                    barLabels: ["4'50\"", "5'00\"", "5'10\"", "5'20\""], xAxisLabels: nil, yLabelFormatter: nil)
        view.selectBar(at: 0)
        let plot = view.bounds.inset(by: style.plotInsets)
        let bubble = try! XCTUnwrap((view.layer.sublayers ?? [])
            .flatMap { $0.sublayers ?? [] }
            .first { $0.name == "bar.selection.bubble" })
        XCTAssertGreaterThanOrEqual(bubble.frame.minX, plot.minX - 0.5, "너무 넓어도 좌측 경계 밖으로 나가지 않음")
    }
}

extension RDBarChartView {
    /// 모든 CATextLayer 문자열(테스트용). textLayerCount와 동일하게 contentLayer(= layer.sublayers[0]) 하위를 순회.
    var allTextLayerStrings: [String] {
        (layer.sublayers ?? [])
            .flatMap { $0.sublayers ?? [] }
            .compactMap { ($0 as? CATextLayer)?.string as? String }
    }
}
