import XCTest
@testable import LumipolGraphUI

final class AreaSilhouetteTests: XCTestCase {
    func testHeightFractionsNormalizesToOwnMinMax() {
        XCTAssertEqual(AreaSilhouette.heightFractions([0, 10, 5]), [0, 1, 0.5])
    }

    func testHeightFractionsEmptyInputReturnsEmpty() {
        XCTAssertEqual(AreaSilhouette.heightFractions([]), [])
    }

    func testHeightFractionsSinglePointIsFlat() {
        XCTAssertEqual(AreaSilhouette.heightFractions([42]), [0])
    }

    func testHeightFractionsAllEqualIsFlatNoDivideByZero() {
        XCTAssertEqual(AreaSilhouette.heightFractions([3, 3, 3]), [0, 0, 0])
    }

    private let plot = PlotArea(bounds: CGRect(x: 0, y: 0, width: 100, height: 100), insets: .zero)
    private let xScale = AxisScale(ticks: [
        AxisTick(value: 0, position: 0), AxisTick(value: 10, position: 1),
    ])!

    func testLayerSpansBottomFractionFromBaseline() {
        // 고도 0→10 (fraction 0→1), areaHeightFraction 0.35 → 봉우리 y = 100 - 35 = 65, 바닥 100
        var style = ChartStyle.default
        style.areaHeightFraction = 0.35
        let layer = AreaSilhouette.layer(
            points: [AreaPoint(x: 0, y: 0), AreaPoint(x: 10, y: 10)],
            xScale: xScale, plotArea: plot, style: style
        )
        XCTAssertEqual(layer?.name, "area.altitude")
        XCTAssertEqual(layer?.path?.boundingBox, CGRect(x: 0, y: 65, width: 100, height: 35))
        XCTAssertNil(layer?.strokeColor)
        XCTAssertEqual(layer?.fillColor, style.areaFillColor.cgColor)
    }

    func testLayerNilForFewerThanTwoPoints() {
        XCTAssertNil(AreaSilhouette.layer(
            points: [AreaPoint(x: 0, y: 5)], xScale: xScale, plotArea: plot, style: .default
        ))
    }
}
