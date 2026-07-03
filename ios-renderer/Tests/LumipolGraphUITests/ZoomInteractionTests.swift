import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class ZoomInteractionTests: XCTestCase {
    private func makeZoomableView() -> RDChartView {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.isZoomEnabled = true
        view.render(TestFixtures.paceAndHeartRate, invertedAxes: [.primary])
        view.layoutIfNeeded()
        return view
    }

    private func xTickValues(_ view: RDChartView) -> [Double] {
        view.chartLayout?.axisTicks
            .first { $0.axis == .x }?.ticks.map(\.value) ?? []
    }

    func testZoomToRangeRelayoutsTicksWithinWindow() {
        let view = makeZoomableView()
        let fullTicks = xTickValues(view)
        view.zoom(toXRange: 1.0...3.0)
        let zoomedTicks = xTickValues(view)
        XCTAssertNotEqual(fullTicks, zoomedTicks)
        zoomedTicks.forEach { XCTAssertTrue((1.0...3.0).contains($0)) }
    }

    func testResetZoomRestoresFullLayout() {
        let view = makeZoomableView()
        let fullTicks = xTickValues(view)
        view.zoom(toXRange: 1.0...3.0)
        view.resetZoom()
        XCTAssertEqual(xTickValues(view), fullTicks)
    }

    func testRenderResetsZoom() {
        let view = makeZoomableView()
        view.zoom(toXRange: 1.0...3.0)
        view.render(TestFixtures.paceAndHeartRate, invertedAxes: [.primary])
        view.layoutIfNeeded()
        let ticks = xTickValues(view)
        XCTAssertTrue(ticks.contains(0.0))  // 전체 보기 tick으로 복귀
    }

    func testZoomIgnoredWhenDisabled() {
        let view = makeZoomableView()
        view.isZoomEnabled = false
        let fullTicks = xTickValues(view)
        view.zoom(toXRange: 1.0...3.0)
        XCTAssertEqual(xTickValues(view), fullTicks)
    }

    func testClipMaskActiveOnlyWhenZoomed() {
        let view = makeZoomableView()
        let clip = view.layer.sublayers?.first { $0.name == "zoom.clip" }
        XCTAssertNil(clip?.mask)
        view.zoom(toXRange: 1.0...3.0)
        XCTAssertNotNil(clip?.mask)
        view.resetZoom()
        XCTAssertNil(clip?.mask)
    }

    func testHorizontalDominantPredicate() {
        XCTAssertTrue(RDChartView.isHorizontalDominant(translation: CGPoint(x: 10, y: 3)))
        XCTAssertFalse(RDChartView.isHorizontalDominant(translation: CGPoint(x: 3, y: 10)))
    }

    func testDoubleTapRecognizerAttachedOnlyWhenZoomEnabled() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        let doubleTaps: (RDChartView) -> Int = { v in
            (v.gestureRecognizers ?? []).compactMap { $0 as? UITapGestureRecognizer }
                .filter { $0.numberOfTapsRequired == 2 }.count
        }
        XCTAssertEqual(doubleTaps(view), 0)  // 기본: 미부착 → 단일 탭 requirement 무효
        view.isZoomEnabled = true
        XCTAssertEqual(doubleTaps(view), 1)
        view.isZoomEnabled = false
        XCTAssertEqual(doubleTaps(view), 0)
    }

    func testSingleTapRecognizerAlwaysAttached() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        let singleTaps = (view.gestureRecognizers ?? []).compactMap { $0 as? UITapGestureRecognizer }
            .filter { $0.numberOfTapsRequired == 1 }
        XCTAssertEqual(singleTaps.count, 1)
    }

    func testLiveTransformKeepsAnchorPointFixed() {
        let view = makeZoomableView()
        let anchorX: CGFloat = 100
        view.applyLiveTransform(scaleX: 2, translationX: 0, anchorX: anchorX)
        let content = (view.layer.sublayers?.first { $0.name == "zoom.clip" })!
            .sublayers!.first { $0.name == "zoom.content" }!
        // 변환 전 anchorX에 있던 점이 변환 후에도 anchorX에 남는다
        let fixed = content.convert(CGPoint(x: anchorX, y: 0), to: view.layer)
        XCTAssertEqual(fixed.x, anchorX, accuracy: 0.5)
        // 확인 사살: 다른 점(x=0)은 anchor에서 멀어지는 방향으로 2배
        let moved = content.convert(CGPoint(x: 0, y: 0), to: view.layer)
        XCTAssertEqual(moved.x, -anchorX, accuracy: 0.5)
    }

    func testShowTouchMarkerIgnoresXOutsideZoomWindow() {
        let view = makeZoomableView()
        view.zoom(toXRange: 1.0...3.0)
        view.showTouchMarker(atX: 4.5)  // 창 밖
        let hasMarker = view.layer.sublayers?.contains { $0.name == "touch.marker" } ?? false
        XCTAssertFalse(hasMarker)
    }

    func testZoomToRangeHidesExistingMarker() {
        let view = makeZoomableView()
        view.showTouchMarker(atX: 2.0)
        view.zoom(toXRange: 1.0...3.0)
        let hasMarker = view.layer.sublayers?.contains { $0.name == "touch.marker" } ?? false
        XCTAssertFalse(hasMarker)
    }
}
