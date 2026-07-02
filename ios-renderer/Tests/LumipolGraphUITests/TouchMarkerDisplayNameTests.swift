import XCTest

@testable import LumipolGraphUI

final class TouchMarkerDisplayNameTests: XCTestCase {
    private func makeData() -> LineChartData {
        LineChartData(
            series: [
                Series(
                    id: "pace",
                    points: [Point(x: 0, y: 5), Point(x: 1, y: 6)],
                    axis: .primary, role: .main
                )
            ],
            referenceLines: [], referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 1, maxTicks: 5)
        )
    }

    private func bubbleTexts(in view: RDChartView) -> [String] {
        func collect(_ layer: CALayer) -> [String] {
            var texts: [String] = []
            if layer.name == "touch.bubble" {
                for sub in layer.sublayers ?? [] {
                    if let text = (sub as? CATextLayer)?.string as? String { texts.append(text) }
                }
            }
            for sub in layer.sublayers ?? [] { texts += collect(sub) }
            return texts
        }
        return collect(view.layer)
    }

    func testBubbleUsesInjectedDisplayName() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.isAnimationEnabled = false
        view.render(makeData(), seriesDisplayNames: ["pace": "페이스"])
        view.layoutIfNeeded()
        view.showTouchMarker(atX: 0.5)
        let texts = bubbleTexts(in: view)
        XCTAssertTrue(texts.contains { $0.hasPrefix("페이스 ") }, "표시명 주입: \(texts)")
    }

    func testBubbleFallsBackToSeriesId() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        view.isAnimationEnabled = false
        view.render(makeData())
        view.layoutIfNeeded()
        view.showTouchMarker(atX: 0.5)
        let texts = bubbleTexts(in: view)
        XCTAssertTrue(texts.contains { $0.hasPrefix("pace ") }, "기본은 raw id: \(texts)")
    }
}
