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

        // bounds 변경 → 실제 리빌드가 일어나는 레이아웃 패스에서도 애니메이션은 재실행되지 않는다.
        view.frame = CGRect(x: 0, y: 0, width: 400, height: 300)
        view.layoutIfNeeded()
        let relaidOut = mainLineLayers(of: view)
        XCTAssertFalse(relaidOut.isEmpty)
        XCTAssertTrue(relaidOut.allSatisfy { $0.animation(forKey: "strokeEnd") == nil })
    }

    func testNoOpLayoutPassReusesExistingLayers() {
        // bounds도 데이터도 변하지 않은 레이아웃 패스(스크롤뷰 임베드·형제 제약 변경 등)는
        // CALayer 트리 전체 파괴·재생성(경로 재계산 + 라벨 재측정)을 반복하면 안 된다.
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.paceAndHeartRate)
        view.layoutIfNeeded()
        let gridBefore = allChartLayers(of: view).first { $0.name == "grid" }
        XCTAssertNotNil(gridBefore)

        view.setNeedsLayout()
        view.layoutIfNeeded()
        let gridAfter = allChartLayers(of: view).first { $0.name == "grid" }
        XCTAssertTrue(gridBefore === gridAfter, "변경 없는 패스는 기존 레이어를 재사용")

        // bounds가 바뀌면 실제 리빌드 (회귀 방지)
        view.frame = CGRect(x: 0, y: 0, width: 400, height: 300)
        view.layoutIfNeeded()
        let gridResized = allChartLayers(of: view).first { $0.name == "grid" }
        XCTAssertNotNil(gridResized)
        XCTAssertFalse(gridBefore === gridResized, "bounds 변경 시에는 재구축")
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
        var backgroundValues: [Double] = []
        func chartView(_ view: RDChartView, didScrubTo valuesBySeriesId: [String: String]) {
            scrubbed.append(valuesBySeriesId)
        }
        func chartViewDidEndScrub(_ view: RDChartView) { endCount += 1 }
        func chartView(_ view: RDChartView, didScrubToBackgroundValue value: Double) {
            backgroundValues.append(value)
        }
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

    func testRelayoutRestoresMarkerWithoutRefiringScrubDelegate() {
        // 마커 표시 중 레이아웃 패스(회전·리사이즈·상위 레이아웃)가 돌면 마커는 복원하되,
        // 사용자 입력이 없으므로 스크럽 콜백(햅틱/애널리틱스 부작용)을 재발화하면 안 된다.
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(
            TestFixtures.fullChart,
            invertedAxes: [.primary],
            labelFormatter: TestFixtures.format,
            backgroundArea: [AreaPoint(x: 0, y: 0), AreaPoint(x: 5, y: 100)]
        )
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 2.4)
        XCTAssertEqual(spy.scrubbed.count, 1)
        XCTAssertEqual(spy.backgroundValues.count, 1)

        view.setNeedsLayout()
        view.layoutIfNeeded()
        XCTAssertTrue((view.layer.sublayers ?? []).contains { $0.name == "touch.marker" }, "마커는 복원")
        XCTAssertEqual(spy.scrubbed.count, 1, "레이아웃 패스는 didScrubTo를 재발화하지 않음")
        XCTAssertEqual(spy.backgroundValues.count, 1, "레이아웃 패스는 배경 값 콜백을 재발화하지 않음")
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

    func testNoEndScrubWhenMarkerCannotBeMadeAndNothingWasShowing() {
        // 확대 창 경계 부근 탭: rawX는 창 안이지만 근접점이 창 밖으로 스냅돼 마커 생성이 실패하는 경우 —
        // 애초에 표시 중인 마커가 없었다면 didScrubTo 없는 endScrub(짝 깨진 콜백)를 보내면 안 된다.
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.isZoomEnabled = true
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        view.zoom(toXRange: 1.2 ... 2.8)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 2.79)  // 창 안이지만 근접점은 3.0(창 밖)으로 스냅
        XCTAssertEqual(spy.scrubbed.count, 0)
        XCTAssertEqual(spy.endCount, 0, "표시된 마커가 없으면 종료 콜백도 없음")
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

    // MARK: - backgroundValue 선형 보간

    func testBackgroundValueClampsToEndpoints() {
        let points = [AreaPoint(x: 1, y: 10), AreaPoint(x: 3, y: 30)]
        // 범위 밖(왼쪽/오른쪽)은 양 끝값으로 클램프
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 0), 10)
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 1), 10)  // 끝점 정확히
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 3), 30)  // 끝점 정확히
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 5), 30)
    }

    func testBackgroundValueInterpolatesMidpoint() {
        let points = [AreaPoint(x: 0, y: 0), AreaPoint(x: 5, y: 100)]
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 2.5)!, 50, accuracy: 1e-9)
        // 세 구간 중 두 번째 구간 내 보간
        let multi = [AreaPoint(x: 0, y: 0), AreaPoint(x: 2, y: 20), AreaPoint(x: 4, y: 0)]
        XCTAssertEqual(RDChartView.backgroundValue(multi, atX: 3)!, 10, accuracy: 1e-9)
    }

    func testBackgroundValueEmptyReturnsNil() {
        XCTAssertNil(RDChartView.backgroundValue([], atX: 1))
    }

    func testBackgroundValueExactInteriorPointAndDuplicateX() {
        // 내부 포인트 정확히 일치 + 같은 x가 연속(dx=0)인 경우 — 탐색 구현 교체 시 회귀 방지.
        let points = [
            AreaPoint(x: 0, y: 0), AreaPoint(x: 1, y: 10),
            AreaPoint(x: 1, y: 20), AreaPoint(x: 2, y: 40),
        ]
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 1)!, 10, accuracy: 1e-9)
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 1.5)!, 30, accuracy: 1e-9)
        // 많은 포인트에서 각 구간 중앙값 검증
        let many = (0...100).map { AreaPoint(x: Double($0), y: Double($0) * 2) }
        XCTAssertEqual(RDChartView.backgroundValue(many, atX: 37.5)!, 75, accuracy: 1e-9)
        XCTAssertEqual(RDChartView.backgroundValue(many, atX: 99)!, 198, accuracy: 1e-9)
    }

    func testBackgroundValueSinglePointReturnsThatY() {
        let points = [AreaPoint(x: 2, y: 42)]
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 0), 42)
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 2), 42)
        XCTAssertEqual(RDChartView.backgroundValue(points, atX: 9), 42)
    }

    // MARK: - 배경 스크럽 델리게이트

    func testScrubReportsBackgroundValueWhenAreaPresent() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(
            TestFixtures.fullChart,
            invertedAxes: [.primary],
            labelFormatter: TestFixtures.format,
            backgroundArea: [AreaPoint(x: 0, y: 0), AreaPoint(x: 5, y: 100)]
        )
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 2.4)  // 근접점 스냅 x=2.5 → 보간값 50
        XCTAssertEqual(spy.backgroundValues.last!, 50, accuracy: 1e-9)
    }

    func testScrubReportsCorrectBackgroundValueWhenAreaPointsUnsorted() {
        // 호출자가 x 내림차순(최신 우선)으로 전달해도 실루엣은 똑같이 그려져 시각적 단서가 없다 —
        // 델리게이트 값도 정렬 순서와 무관하게 올바라야 한다.
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(
            TestFixtures.fullChart,
            invertedAxes: [.primary],
            labelFormatter: TestFixtures.format,
            backgroundArea: [AreaPoint(x: 5, y: 100), AreaPoint(x: 0, y: 0)]  // 내림차순
        )
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 2.4)  // 근접점 스냅 x=2.5 → 보간값 50
        XCTAssertEqual(spy.backgroundValues.last!, 50, accuracy: 1e-9)
    }

    func testScrubOmitsBackgroundValueWhenNoArea() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 2.4)
        XCTAssertFalse(spy.scrubbed.isEmpty, "라인 값은 여전히 전달")
        XCTAssertTrue(spy.backgroundValues.isEmpty, "backgroundArea 없으면 배경 콜백 없음")
    }
}
