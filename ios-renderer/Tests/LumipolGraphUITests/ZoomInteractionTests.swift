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
}
