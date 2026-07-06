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
                isPartial: i == barCount - 1
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
            BarLayout(index: Int32(0), value: 250, heightFraction: 0.8, colorRole: .faster, isPartial: false),
            BarLayout(index: Int32(1), value: 300, heightFraction: 0.5, colorRole: .onTarget, isPartial: false),
            BarLayout(index: Int32(2), value: 350, heightFraction: 0.3, colorRole: .slower, isPartial: true),
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
}
