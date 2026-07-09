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

    func testLayerClampsPointsOutsideXDomainToPlotRect() {
        // 시리즈 x-도메인(0~10)보다 넓은 고도 데이터(-5~15) — 1x에서는 클립 마스크가 없으므로
        // 실루엣이 플롯 밖(축 라벨 영역)으로 번지지 않게 좌표 자체를 플롯 영역으로 클램프해야 한다.
        let layer = AreaSilhouette.layer(
            points: [AreaPoint(x: -5, y: 0), AreaPoint(x: 5, y: 10), AreaPoint(x: 15, y: 0)],
            xScale: xScale, plotArea: plot, style: .default
        )
        let box = layer!.path!.boundingBox
        XCTAssertGreaterThanOrEqual(box.minX, plot.rect.minX)
        XCTAssertLessThanOrEqual(box.maxX, plot.rect.maxX)
    }

    func testLayerNilForFewerThanTwoPoints() {
        XCTAssertNil(AreaSilhouette.layer(
            points: [AreaPoint(x: 0, y: 5)], xScale: xScale, plotArea: plot, style: .default
        ))
    }
}
