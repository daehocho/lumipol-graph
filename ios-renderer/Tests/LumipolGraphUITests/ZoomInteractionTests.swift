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

    func testPinchZoomInStillWorksAfterMomentarilyPassingThroughFullDomain() {
        // 한 번의 핀치 안에서 창이 잠깐 전체 구간(1x)으로 클램프됐다가 다시 확대되는 시나리오 —
        // 1x를 지나는 순간 줌 상태가 버려지면 같은 제스처의 나머지 프레임이 전부 무시된다.
        let view = makeZoomableView()
        let fullTicks = xTickValues(view)
        view.pinchBegan()
        view.pinchChanged(cumulativeScale: 0.9, anchor: 0.5)  // 1x에서 축소 → 전체 구간으로 클램프
        view.pinchChanged(cumulativeScale: 2.0, anchor: 0.5)  // 같은 제스처로 다시 확대
        view.pinchEnded()
        XCTAssertNotEqual(xTickValues(view), fullTicks, "핀치 확대가 살아 있어야 함")
    }

    func testPinchEndAtFullDomainRestoresCleanState() {
        // 제스처가 1x에서 끝나면 내부 줌 상태도 초기화돼 다음 핀치가 정상 동작해야 한다.
        let view = makeZoomableView()
        let fullTicks = xTickValues(view)
        view.pinchBegan()
        view.pinchChanged(cumulativeScale: 0.5, anchor: 0.5)  // 축소 → 전체 구간
        view.pinchEnded()
        XCTAssertEqual(xTickValues(view), fullTicks)
        // 다음 핀치로 확대 가능
        view.pinchBegan()
        view.pinchChanged(cumulativeScale: 2.0, anchor: 0.5)
        view.pinchEnded()
        XCTAssertNotEqual(xTickValues(view), fullTicks)
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

    func testShowTouchMarkerIgnoresXOutsideZoomWindow() {
        let view = makeZoomableView()
        view.zoom(toXRange: 1.0...3.0)
        view.showTouchMarker(atX: 4.5)  // 창 밖
        let hasMarker = view.layer.sublayers?.contains { $0.name == "touch.marker" } ?? false
        XCTAssertFalse(hasMarker)
    }

    func testShowTouchMarkerToleratesFloatEdgeJustOutsideWindow() {
        // 창 오른쪽 끝 스크럽: 정규화 1 → 도메인 역산이 반올림으로 상한을 수 ulp 넘을 수 있다 —
        // TouchMarker와 동일하게 epsilon 이내는 창 안으로 클램프해야 끝 스크럽이 침묵 드롭되지 않는다.
        let view = makeZoomableView()
        view.zoom(toXRange: 1.0...3.0)
        view.showTouchMarker(atX: 3.0 + 3.0.ulp * 4)  // 상한 + 수 ulp
        let hasMarker = view.layer.sublayers?.contains { $0.name == "touch.marker" } ?? false
        XCTAssertTrue(hasMarker, "부동소수점 경계 오차는 드롭이 아니라 클램프")
    }

    func testZoomToRangeHidesExistingMarker() {
        let view = makeZoomableView()
        view.showTouchMarker(atX: 2.0)
        view.zoom(toXRange: 1.0...3.0)
        let hasMarker = view.layer.sublayers?.contains { $0.name == "touch.marker" } ?? false
        XCTAssertFalse(hasMarker)
    }

    func testLongPressRecognizerAttachedWithDuration() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        let longPresses = (view.gestureRecognizers ?? [])
            .compactMap { $0 as? UILongPressGestureRecognizer }
        XCTAssertEqual(longPresses.count, 1)
        XCTAssertEqual(longPresses.first?.minimumPressDuration ?? 0, 0.5, accuracy: 1e-9)
    }
}
