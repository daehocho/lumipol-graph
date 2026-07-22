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

    // 장거리(42km≈43스플릿) 라벨 겹침: 슬롯보다 넓은 라벨은 stride로 솎아낸다.
    func testBarLabelsThinnedWhenTooNarrow() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
        let labels = Array(repeating: "5'30\"", count: 20)
        view.render(sampleLayout(barCount: 20), style: .default, barLabels: labels,
                    xAxisLabels: nil, yLabelFormatter: nil)
        view.layoutIfNeeded()
        let shown = view.allTextLayerStrings.filter { $0 == "5'30\"" }.count
        XCTAssertLessThan(shown, 20, "겹침 방지 솎아내기 기대")
        XCTAssertGreaterThan(shown, 0)
    }

    // 리뷰 #1: 솎아내도 첫·마지막(피니시) 라벨은 항상 표시.
    func testAlwaysShowsFirstAndLastBarLabel() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
        let labels = (0..<20).map { "\($0)'30\"" }
        view.render(sampleLayout(barCount: 20), style: .default, barLabels: labels,
                    xAxisLabels: nil, yLabelFormatter: nil)
        view.layoutIfNeeded()
        let strings = view.allTextLayerStrings
        XCTAssertTrue(strings.contains("0'30\""), "첫 라벨 표시")
        XCTAssertTrue(strings.contains("19'30\""), "마지막(피니시) 라벨 표시")
        XCTAssertLessThan(strings.filter { $0.hasSuffix("'30\"") }.count, 20, "여전히 솎아냄")
    }

    func testBarLabelsAllShownWhenTheyFit() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 600, height: 200))
        let labels = Array(repeating: "1", count: 4)
        view.render(sampleLayout(barCount: 4), style: .default, barLabels: labels,
                    xAxisLabels: nil, yLabelFormatter: nil)
        view.layoutIfNeeded()
        XCTAssertEqual(view.allTextLayerStrings.filter { $0 == "1" }.count, 4)
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
}

extension RDBarChartView {
    /// 모든 CATextLayer 문자열(테스트용). textLayerCount와 동일하게 contentLayer(= layer.sublayers[0]) 하위를 순회.
    var allTextLayerStrings: [String] {
        (layer.sublayers ?? [])
            .flatMap { $0.sublayers ?? [] }
            .compactMap { ($0 as? CATextLayer)?.string as? String }
    }
}
