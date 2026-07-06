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
}
