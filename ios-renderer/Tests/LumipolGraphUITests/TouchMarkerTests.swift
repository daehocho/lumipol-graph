import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class TouchMarkerTests: XCTestCase {
    // fullChart를 엔진에 통과시킨 실제 레이아웃으로 마커를 만든다.
    private let data = TestFixtures.fullChart
    private var layout: LineChartLayout { LineChartEngine.shared.layout(data: data) }
    private let plotArea = PlotArea(
        bounds: CGRect(x: 0, y: 0, width: 390, height: 300),
        insets: ChartStyle.default.plotInsets,
        invertedAxes: [.primary]
    )

    private func makeMarker(atRawX rawX: Double) -> CALayer? {
        TouchMarker.makeLayer(atRawX: rawX, context: TouchMarker.Context(
            data: data, layout: layout, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
    }

    func testMarkerContainsLineDotsAndBubble() {
        let marker = makeMarker(atRawX: 2.4)
        XCTAssertEqual(marker?.name, "touch.marker")
        let names = marker?.sublayers?.compactMap(\.name) ?? []
        XCTAssertTrue(names.contains("touch.line"))
        XCTAssertTrue(names.contains("touch.dot.pace"))
        XCTAssertTrue(names.contains("touch.dot.pace_prev"))
        XCTAssertTrue(names.contains("touch.dot.hr"))
        XCTAssertTrue(names.contains("touch.bubble"))
    }

    func testBubbleShowsFormattedValuesPerSeries() {
        // rawX 2.4 → 근접점 x=2.5 (인덱스 5): pace 5.5 → "5'30\"", hr 166 → "166"
        let marker = makeMarker(atRawX: 2.4)
        let bubble = marker?.sublayers?.first { $0.name == "touch.bubble" }
        let lines = bubble?.sublayers?.compactMap { ($0 as? CATextLayer)?.string as? String } ?? []
        XCTAssertEqual(lines.count, 3)
        XCTAssertTrue(lines.contains { $0.contains("pace") && $0.contains("5'30\"") })
        XCTAssertTrue(lines.contains { $0.contains("hr") && $0.contains("166") })
    }

    func testDotSitsOnSnappedXAndBubbleStaysInsidePlot() {
        let marker = makeMarker(atRawX: 0.0) // 왼쪽 끝 — 말풍선 클램프 확인
        let line = marker?.sublayers?.first { $0.name == "touch.line" } as? CAShapeLayer
        let dot = marker?.sublayers?.first { $0.name == "touch.dot.pace" } as? CAShapeLayer
        XCTAssertNotNil(line?.path)
        // 점과 수직선의 x가 일치(스냅된 x)
        XCTAssertEqual(dot!.path!.boundingBox.midX, line!.path!.boundingBox.midX, accuracy: 0.5)
        let bubble = marker?.sublayers?.first { $0.name == "touch.bubble" }
        XCTAssertGreaterThanOrEqual(bubble!.frame.minX, plotArea.rect.minX - 0.5)
    }

    func testReturnsNilWhenAxisScaleUnavailable() {
        // 축 tick이 없는 빈 레이아웃 → 역산 불능 → nil
        let emptyLayout = LineChartLayout(
            series: [], axisTicks: [], refLines: [], refBands: [], markers: [],
            stats: Stats(perSeries: [], segments: [], segmentSeriesId: nil)
        )
        let marker = TouchMarker.makeLayer(atRawX: 1.0, context: TouchMarker.Context(
            data: data, layout: emptyLayout, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
        XCTAssertNil(marker)
    }
}
