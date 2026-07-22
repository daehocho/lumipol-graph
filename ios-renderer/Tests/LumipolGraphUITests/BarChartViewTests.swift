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

    func testBarColorMatchesRoleAndPartialIsDimmed() {
        var style = ChartStyle.default
        style.barColors = [.faster: .red, .onTarget: .green, .slower: .blue]
        let bars = [
            BarLayout(index: Int32(0), value: 250, heightFraction: 0.8, colorRole: .faster, isPartial: false, endMinutes: nil),
            BarLayout(index: Int32(1), value: 300, heightFraction: 0.5, colorRole: .onTarget, isPartial: false, endMinutes: nil),
            BarLayout(index: Int32(2), value: 350, heightFraction: 0.3, colorRole: .slower, isPartial: true, endMinutes: nil),
        ]
        let layout = BarChartLayout(bars: bars, yTicks: [], referenceLinePosition: nil)
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.render(layout, style: style)
        XCTAssertEqual(view.barLayers.count, 3)
        XCTAssertEqual(view.barLayers[0].backgroundColor, UIColor.red.cgColor)
        XCTAssertEqual(view.barLayers[1].backgroundColor, UIColor.green.cgColor)
        XCTAssertEqual(view.barLayers[2].backgroundColor, UIColor.blue.cgColor)
        XCTAssertLessThan(view.barLayers[2].opacity, 1.0)   // 부분막대 흐림
        XCTAssertEqual(view.barLayers[0].opacity, 1.0)
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

    func testSelectionShowsGuideAndCallout() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        var style = ChartStyle.default
        style.barShowYAxisLabels = false
        view.render(sampleLayout(barCount: 4), style: style,
                    barLabels: ["4'50\"", "5'00\"", "5'10\"", "5'20\""], xAxisLabels: nil, yLabelFormatter: nil)
        view.selectBar(at: 2)
        XCTAssertTrue(hasSelectionGuide(view), "수직 가이드선")
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
}

extension RDBarChartView {
    /// 모든 CATextLayer 문자열(테스트용). textLayerCount와 동일하게 contentLayer(= layer.sublayers[0]) 하위를 순회.
    var allTextLayerStrings: [String] {
        (layer.sublayers ?? [])
            .flatMap { $0.sublayers ?? [] }
            .compactMap { ($0 as? CATextLayer)?.string as? String }
    }
}
