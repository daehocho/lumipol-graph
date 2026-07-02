import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class RDChartViewTests: XCTestCase {
    private func chartLayerNames(of view: RDChartView) -> [String] {
        (view.layer.sublayers ?? []).compactMap(\.name)
    }

    func testRenderBuildsLayersAfterLayout() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        let names = chartLayerNames(of: view)
        XCTAssertTrue(names.contains("series.main.pace"))
        XCTAssertTrue(names.contains("series.main.hr"))
        XCTAssertTrue(names.contains("series.ghost.pace_prev"))
        XCTAssertTrue(names.contains("refLine.0"))
        XCTAssertTrue(names.contains("band.0"))
        XCTAssertTrue(names.contains("axisLabels.ySecondary"))
    }

    func testZeroBoundsRenderIsDeferredUntilLayout() {
        let view = RDChartView(frame: .zero)
        view.isAnimationEnabled = false
        view.render(TestFixtures.paceOnly)
        XCTAssertTrue(chartLayerNames(of: view).isEmpty)
        // 프레임이 잡히면 layoutSubviews가 레이어를 만든다
        view.frame = CGRect(x: 0, y: 0, width: 390, height: 300)
        view.layoutIfNeeded()
        XCTAssertTrue(chartLayerNames(of: view).contains("series.main.pace"))
    }

    func testReRenderReplacesLayers() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.fullChart)
        view.layoutIfNeeded()
        view.render(TestFixtures.paceOnly)
        view.layoutIfNeeded()
        let names = chartLayerNames(of: view)
        XCTAssertTrue(names.contains("series.main.pace"))
        XCTAssertFalse(names.contains("series.ghost.pace_prev"))
        XCTAssertEqual(names.filter { $0 == "series.main.pace" }.count, 1)
    }

    func testTouchMarkerSurvivesRelayout() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        view.showTouchMarker(atX: 2.4)
        view.setNeedsLayout()
        view.layoutIfNeeded()
        XCTAssertTrue((view.layer.sublayers ?? []).contains { $0.name == "touch.marker" })
    }

    private func mainLineLayers(of view: RDChartView) -> [CAShapeLayer] {
        (view.layer.sublayers ?? []).compactMap { $0 as? CAShapeLayer }
            .filter { $0.name?.hasPrefix("series.main.") == true }
    }

    func testEntranceAnimationRunsOnlyOnFirstLayoutAfterRender() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.render(TestFixtures.paceOnly) // isAnimationEnabled 기본값 true
        view.layoutIfNeeded()
        let animated = mainLineLayers(of: view)
        XCTAssertFalse(animated.isEmpty)
        XCTAssertTrue(animated.allSatisfy { $0.animation(forKey: "strokeEnd") != nil })

        view.setNeedsLayout()
        view.layoutIfNeeded()
        let relaidOut = mainLineLayers(of: view)
        XCTAssertFalse(relaidOut.isEmpty)
        XCTAssertTrue(relaidOut.allSatisfy { $0.animation(forKey: "strokeEnd") == nil })
    }

    func testRenderResetsTouchMarker() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        view.showTouchMarker(atX: 2.4)
        view.render(TestFixtures.paceOnly)
        view.layoutIfNeeded()
        XCTAssertFalse((view.layer.sublayers ?? []).contains { $0.name == "touch.marker" })
    }
}
