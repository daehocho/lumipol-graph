import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class RDChartViewTests: XCTestCase {
    /// 루트(축 라벨·터치 마커)와 `zoom.clip > zoom.content`(시리즈·그리드 등) 양쪽을 합쳐 반환한다.
    private func allChartLayers(of view: RDChartView) -> [CALayer] {
        let root = view.layer.sublayers ?? []
        let content = root
            .first { $0.name == "zoom.clip" }?
            .sublayers?.first { $0.name == "zoom.content" }?
            .sublayers ?? []
        return root + content
    }

    private func chartLayerNames(of view: RDChartView) -> [String] {
        allChartLayers(of: view).compactMap(\.name)
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
        allChartLayers(of: view).compactMap { $0 as? CAShapeLayer }
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

    func testPlotContentIsGroupedUnderContentContainerAndLabelsStayAtRoot() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.paceAndHeartRate)
        view.layoutIfNeeded()

        let rootNames = view.layer.sublayers?.compactMap(\.name) ?? []
        // 축 라벨은 루트에 남는다 (줌 변환 제외 대상)
        XCTAssertTrue(rootNames.contains("axisLabels.x"))
        XCTAssertTrue(rootNames.contains { $0.hasPrefix("axisLabels.y") })
        // 시리즈·그리드는 루트가 아니라 clip > content 컨테이너 아래
        XCTAssertFalse(rootNames.contains { $0.hasPrefix("series.") || $0 == "grid" })
        let clip = view.layer.sublayers?.first { $0.name == "zoom.clip" }
        let content = clip?.sublayers?.first { $0.name == "zoom.content" }
        let contentNames = content?.sublayers?.compactMap(\.name) ?? []
        XCTAssertTrue(contentNames.contains { $0.hasPrefix("series.main.") })
        XCTAssertTrue(contentNames.contains("grid"))
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

    // MARK: - Scrub delegate

    private final class SpyScrubDelegate: RDChartScrubDelegate {
        var scrubbed: [[String: String]] = []
        var endCount = 0
        func chartView(_ view: RDChartView, didScrubTo valuesBySeriesId: [String: String]) {
            scrubbed.append(valuesBySeriesId)
        }
        func chartViewDidEndScrub(_ view: RDChartView) { endCount += 1 }
    }

    func testShowTouchMarkerReportsValuesToDelegate() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 2.4)
        XCTAssertEqual(spy.scrubbed.last?["pace"], "5'30\"")
        XCTAssertEqual(spy.scrubbed.last?["hr"], "166")
        XCTAssertEqual(spy.endCount, 0, "표시 중에는 종료 콜백 없음")
    }

    func testHideTouchMarkerReportsEndOnceWhenShown() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 2.4)
        view.hideTouchMarker()
        XCTAssertEqual(spy.endCount, 1)
    }

    func testHideTouchMarkerNoEndWhenNothingShown() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.hideTouchMarker()
        XCTAssertEqual(spy.endCount, 0, "표시된 마커가 없으면 종료 콜백도 없음")
    }

    func testScrubAtLocationReportsValuesWhenZoomed() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.isZoomEnabled = true
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        view.zoom(toXRange: 1.0 ... 3.0)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.scrub(at: CGPoint(x: 195, y: 150))  // 플롯(≈44..346) 중앙 부근
        XCTAssertTrue((view.layer.sublayers ?? []).contains { $0.name == "touch.marker" })
        XCTAssertFalse(spy.scrubbed.isEmpty, "확대 창에서도 스크럽 값이 전달돼야 함")
    }

    // MARK: - Background area

    func testBackgroundAreaDrawnBottomMostUnderContent() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(
            TestFixtures.paceOnly,
            backgroundArea: [AreaPoint(x: 0, y: 0), AreaPoint(x: 5, y: 100)]
        )
        view.layoutIfNeeded()
        let content = view.layer.sublayers?
            .first { $0.name == "zoom.clip" }?
            .sublayers?.first { $0.name == "zoom.content" }
        let contentNames = content?.sublayers?.compactMap(\.name) ?? []
        XCTAssertEqual(contentNames.first, "area.altitude", "고도 실루엣은 콘텐츠 최하단")
        XCTAssertTrue(contentNames.contains("series.main.pace"))
    }

    func testNoBackgroundAreaOmitsAreaLayer() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.paceOnly)
        view.layoutIfNeeded()
        XCTAssertFalse(chartLayerNames(of: view).contains("area.altitude"))
    }
}
