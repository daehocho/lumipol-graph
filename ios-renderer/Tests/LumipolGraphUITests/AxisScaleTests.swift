import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class AxisScaleTests: XCTestCase {
    func testMapsValueAndPositionLinearly() {
        let scale = AxisScale(ticks: [
            AxisTick(value: 0, position: 0),
            AxisTick(value: 2, position: 0.5),
            AxisTick(value: 4, position: 1),
        ])
        XCTAssertNotNil(scale)
        XCTAssertEqual(scale!.value(atPosition: 0.75), 3, accuracy: 1e-9)
        XCTAssertEqual(scale!.position(ofValue: 1), 0.25, accuracy: 1e-9)
    }

    func testNonZeroBasePosition() {
        let scale = AxisScale(ticks: [
            AxisTick(value: 10, position: 0.2),
            AxisTick(value: 20, position: 0.8),
        ])
        XCTAssertEqual(scale!.value(atPosition: 0.5), 15, accuracy: 1e-9)
        XCTAssertEqual(scale!.position(ofValue: 12.5), 0.35, accuracy: 1e-9)
    }

    func testReturnsNilForDegenerateTicks() {
        XCTAssertNil(AxisScale(ticks: []))
        XCTAssertNil(AxisScale(ticks: [AxisTick(value: 1, position: 0.5)]))
        XCTAssertNil(AxisScale(ticks: [
            AxisTick(value: 1, position: 0.5),
            AxisTick(value: 2, position: 0.5),
        ]))
        XCTAssertNil(AxisScale(ticks: [
            AxisTick(value: 3, position: 0),
            AxisTick(value: 3, position: 1),
        ]))
    }
}
