import XCTest
@testable import LumipolGraphUI

final class ZoomStateTests: XCTestCase {
    private func makeState() -> ZoomState { ZoomState(fullDomain: 0...10) }

    func testInitialStateIsFullDomain() {
        let state = makeState()
        XCTAssertEqual(state.window, 0...10)
        XCTAssertFalse(state.isZoomed)
        XCTAssertEqual(state.scale, 1.0, accuracy: 1e-9)
    }

    func testPinchInAtCenterHalvesWindowAroundCenter() {
        var state = makeState()
        state.pinch(by: 2.0, anchor: 0.5, maxScale: 10)
        XCTAssertEqual(state.window.lowerBound, 2.5, accuracy: 1e-9)
        XCTAssertEqual(state.window.upperBound, 7.5, accuracy: 1e-9)
        XCTAssertEqual(state.scale, 2.0, accuracy: 1e-9)
    }

    func testPinchAnchorValueStaysPut() {
        var state = makeState()
        // anchor 0.25 → 도메인 값 2.5가 확대 후에도 창의 25% 지점에 남는다
        state.pinch(by: 2.0, anchor: 0.25, maxScale: 10)
        let anchorValue = state.window.lowerBound
            + 0.25 * (state.window.upperBound - state.window.lowerBound)
        XCTAssertEqual(anchorValue, 2.5, accuracy: 1e-9)
    }

    func testPinchClampsAtMaxScale() {
        var state = makeState()
        state.pinch(by: 100.0, anchor: 0.5, maxScale: 10)
        XCTAssertEqual(state.scale, 10.0, accuracy: 1e-9)
    }

    func testPinchOutClampsToFullDomain() {
        var state = makeState()
        state.pinch(by: 2.0, anchor: 0.5, maxScale: 10)
        state.pinch(by: 0.1, anchor: 0.5, maxScale: 10)
        XCTAssertEqual(state.window, 0...10)
        XCTAssertFalse(state.isZoomed)
    }

    func testPanMovesWindowOppositeToDragAndClamps() {
        var state = makeState()
        state.pinch(by: 2.0, anchor: 0.5, maxScale: 10)  // 2.5...7.5
        state.pan(byFraction: 0.2)  // 오른쪽 드래그 → 이전(왼쪽) 구간으로
        XCTAssertEqual(state.window.lowerBound, 1.5, accuracy: 1e-9)
        state.pan(byFraction: 10.0)  // 크게 드래그 → 왼쪽 끝 클램프
        XCTAssertEqual(state.window.lowerBound, 0.0, accuracy: 1e-9)
        state.pan(byFraction: -10.0)  // 반대로 → 오른쪽 끝 클램프
        XCTAssertEqual(state.window.upperBound, 10.0, accuracy: 1e-9)
    }

    func testSetWindowClampsToFullDomain() {
        var state = makeState()
        state.setWindow(8...13)
        XCTAssertEqual(state.window, 5...10)  // 폭 5 유지, 오른쪽 끝 클램프
    }

    func testSetWindowWiderThanFullDomainClampsToFull() {
        var state = makeState()
        state.setWindow(-5...20)
        XCTAssertEqual(state.window, 0...10)
    }

    func testResetRestoresFullDomain() {
        var state = makeState()
        state.pinch(by: 3.0, anchor: 0.3, maxScale: 10)
        state.reset()
        XCTAssertEqual(state.window, 0...10)
    }

    func testZeroOrNegativePinchScaleIsIgnored() {
        var state = makeState()
        state.pinch(by: 0, anchor: 0.5, maxScale: 10)
        XCTAssertEqual(state.window, 0...10)
    }

    // MARK: - Live pinch (기준 창 + 누적 배율)

    func testPinchFromStartIsCumulativeAndAnchored() {
        var state = makeState()
        let start = state.window  // 0...10
        state.pinch(from: start, cumulativeScale: 2.0, anchor: 0.5, maxScale: 10)
        XCTAssertEqual(state.window.lowerBound, 2.5, accuracy: 1e-9)
        XCTAssertEqual(state.window.upperBound, 7.5, accuracy: 1e-9)
        // 같은 기준 창에서 배율만 키우면 누적(드리프트 없음)
        state.pinch(from: start, cumulativeScale: 4.0, anchor: 0.5, maxScale: 10)
        XCTAssertEqual(state.window.lowerBound, 3.75, accuracy: 1e-9)
        XCTAssertEqual(state.window.upperBound, 6.25, accuracy: 1e-9)
    }

    func testPinchFromFullZoomOutStaysFull() {
        var state = makeState()
        state.pinch(from: state.window, cumulativeScale: 0.5, anchor: 0.5, maxScale: 10)
        XCTAssertEqual(state.window, 0...10)
        XCTAssertFalse(state.isZoomed)
    }

    func testPinchFromRespectsMaxScale() {
        var state = makeState()
        state.pinch(from: state.window, cumulativeScale: 100.0, anchor: 0.5, maxScale: 10)
        XCTAssertEqual(state.scale, 10.0, accuracy: 1e-9)
    }
}
