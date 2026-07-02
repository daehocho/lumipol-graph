import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class PlotAreaTests: XCTestCase {
    // rect = (x: 20, y: 10, w: 100, h: 200)
    private let area = PlotArea(
        bounds: CGRect(x: 0, y: 0, width: 120, height: 220),
        insets: UIEdgeInsets(top: 10, left: 20, bottom: 10, right: 0)
    )

    func testXMapsIntoPlotRect() {
        XCTAssertEqual(area.x(0.0), 20)
        XCTAssertEqual(area.x(0.5), 70)
        XCTAssertEqual(area.x(1.0), 120)
    }

    func testNormalAxisPutsMaxValueAtTop() {
        // 정상 축: ny=1(축 최대) → 위(minY), ny=0(축 최소) → 아래(maxY)
        XCTAssertEqual(area.y(1.0, axis: .primary), 10)
        XCTAssertEqual(area.y(0.0, axis: .primary), 210)
        XCTAssertEqual(area.y(0.25, axis: .primary), 160)
    }

    func testInvertedAxisPutsMinValueAtTop() {
        let inverted = PlotArea(
            bounds: CGRect(x: 0, y: 0, width: 120, height: 220),
            insets: UIEdgeInsets(top: 10, left: 20, bottom: 10, right: 0),
            invertedAxes: [.primary]
        )
        // 반전 축(페이스): ny=0(축 최소 = 빠른 페이스) → 위
        XCTAssertEqual(inverted.y(0.0, axis: .primary), 10)
        XCTAssertEqual(inverted.y(1.0, axis: .primary), 210)
        // 반전 대상이 아닌 축은 정상 방향 유지
        XCTAssertEqual(inverted.y(0.0, axis: .secondary), 210)
    }

    func testPointCombinesXAndY() {
        let p = area.point(NormalizedPoint(x: 0.5, y: 0.5), axis: .primary)
        XCTAssertEqual(p, CGPoint(x: 70, y: 110))
    }

    func testNormalizedXInvertsAndClamps() {
        XCTAssertEqual(area.normalizedX(at: 20), 0.0, accuracy: 1e-9)
        XCTAssertEqual(area.normalizedX(at: 70), 0.5, accuracy: 1e-9)
        XCTAssertEqual(area.normalizedX(at: 120), 1.0, accuracy: 1e-9)
        XCTAssertEqual(area.normalizedX(at: -5), 0.0, accuracy: 1e-9)
        XCTAssertEqual(area.normalizedX(at: 999), 1.0, accuracy: 1e-9)
    }

    func testZeroSizeBoundsIsNotRenderable() {
        let zero = PlotArea(bounds: .zero, insets: .zero)
        XCTAssertFalse(zero.isRenderable)
        XCTAssertTrue(area.isRenderable)
    }
}
