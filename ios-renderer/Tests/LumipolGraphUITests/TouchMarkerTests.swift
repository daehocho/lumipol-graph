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

    private func makeResult(atRawX rawX: Double) -> TouchMarker.Result? {
        TouchMarker.make(atRawX: rawX, context: TouchMarker.Context(
            data: data, layout: layout, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
    }

    func testMarkerContainsLineAndDotsButNoBubble() {
        let result = makeResult(atRawX: 2.4)
        XCTAssertEqual(result?.layer.name, "touch.marker")
        let names = result?.layer.sublayers?.compactMap(\.name) ?? []
        XCTAssertTrue(names.contains("touch.line"))
        XCTAssertTrue(names.contains("touch.dot.pace"))
        XCTAssertTrue(names.contains("touch.dot.pace_prev"))
        XCTAssertTrue(names.contains("touch.dot.hr"))
        XCTAssertFalse(names.contains("touch.bubble"), "말풍선은 제거됨")
    }

    func testReturnsFormattedValuesPerSeries() {
        // rawX 2.4 → 근접점 x=2.5 (인덱스 5): pace 5.5 → "5'30\"", hr 166 → "166"
        let values = makeResult(atRawX: 2.4)?.valuesBySeriesId ?? [:]
        XCTAssertEqual(values["pace"], "5'30\"")
        XCTAssertEqual(values["hr"], "166")
    }

    func testDotSitsOnSnappedX() {
        let result = makeResult(atRawX: 0.0)
        let line = result?.layer.sublayers?.first { $0.name == "touch.line" } as? CAShapeLayer
        let dot = result?.layer.sublayers?.first { $0.name == "touch.dot.pace" } as? CAShapeLayer
        XCTAssertNotNil(line?.path)
        // 점과 수직선의 x가 일치(스냅된 x)
        XCTAssertEqual(dot!.path!.boundingBox.midX, line!.path!.boundingBox.midX, accuracy: 0.5)
    }

    func testMarkerShownAtDomainUpperBound() {
        // 도메인 상한(마지막 포인트 x=5.0) — 정규화 x가 반올림으로 1을 수 ulp 넘어도
        // 마커가 침묵 드롭되지 않아야 한다 (경계 클램프 회귀 방지).
        let result = makeResult(atRawX: 5.0)
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.layer.name, "touch.marker")
    }

    func testReturnsNilWhenAxisScaleUnavailable() {
        // 축 tick이 없는 빈 레이아웃 → 역산 불능 → nil
        let emptyLayout = LineChartLayout(
            series: [], axisTicks: [], refLines: [], refBands: [], markers: [],
            stats: Stats(perSeries: [], segments: [], segmentSeriesId: nil)
        )
        let result = TouchMarker.make(atRawX: 1.0, context: TouchMarker.Context(
            data: data, layout: emptyLayout, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
        XCTAssertNil(result)
    }
}
