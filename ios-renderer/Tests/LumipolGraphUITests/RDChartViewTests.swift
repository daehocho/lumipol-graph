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
        XCTAssertFalse(names.contains("series.main.hr"))
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

    func testReRenderDoesNotReplayEntranceAnimation() {
        // 등장 애니메이션은 뷰 수명당 1회(최초 render) — 스트리밍 데이터 갱신마다
        // 라인이 0%부터 다시 그려지면 안 된다 (Android RDLineChart와 동일 계약).
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.render(TestFixtures.paceOnly) // isAnimationEnabled 기본값 true
        view.layoutIfNeeded()
        XCTAssertTrue(mainLineLayers(of: view).allSatisfy { $0.animation(forKey: "strokeEnd") != nil })

        view.render(TestFixtures.paceAndHeartRate) // 데이터 갱신 재렌더
        view.layoutIfNeeded()
        let reRendered = mainLineLayers(of: view)
        XCTAssertFalse(reRendered.isEmpty)
        XCTAssertTrue(
            reRendered.allSatisfy { $0.animation(forKey: "strokeEnd") == nil },
            "데이터 갱신 재렌더는 등장 애니메이션을 재생하지 않음"
        )
    }

    func testEnablingAnimationAfterFirstRenderDoesNotAnimate() {
        // 최초 render가 애니 비활성이었으면 이후 활성화해도 재생 없음 —
        // Android(컴포지션 시점 animateEntrance 고정)와 동일 의미론.
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.paceOnly)
        view.layoutIfNeeded()
        view.isAnimationEnabled = true
        view.render(TestFixtures.paceAndHeartRate)
        view.layoutIfNeeded()
        XCTAssertTrue(mainLineLayers(of: view).allSatisfy { $0.animation(forKey: "strokeEnd") == nil })
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

    func testRenderWhileMarkerShownNotifiesScrubEndOnce() {
        // 스크럽(마커 표시) 중 데이터 갱신 — didScrubTo가 발화된 상태에서 마커가 사라지므로
        // 종료를 1회 통지해 콜백 짝을 보존한다 (Android 제스처 경로 endScrub와 동일 계약).
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 2.4)
        view.render(TestFixtures.paceOnly)
        XCTAssertEqual(spy.endCount, 1, "표시 중이던 마커가 재렌더로 사라지면 종료 통지")

        // 마커가 없을 때의 재렌더는 종료를 통지하지 않는다 (짝 깨진 콜백 금지).
        view.layoutIfNeeded()
        view.render(TestFixtures.paceAndHeartRate)
        XCTAssertEqual(spy.endCount, 1, "마커 없는 재렌더는 종료 통지 없음")
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
        // 창 안에 스냅할 점이 없어 마커 생성이 실패하는 경우(포인트 간격 0.5 사이 갭으로 줌) —
        // 애초에 표시 중인 마커가 없었다면 didScrubTo 없는 endScrub(짝 깨진 콜백)를 보내면 안 된다.
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.isZoomEnabled = true
        view.render(TestFixtures.fullChart, invertedAxes: [.primary], labelFormatter: TestFixtures.format)
        view.layoutIfNeeded()
        view.zoom(toXRange: 1.6 ... 1.9)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 1.7)  // 창 안에 스냅할 점 없음(갭 줌)
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

    // 배경 area 보간(클램프·중복 x·빈 입력)의 단위 테스트는 0.9.0에서 코어로 이관 —
    // core commonTest `AreaInterpolationTest` 참고. 여기는 델리게이트 통합 경로만 검증한다.

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

    // MARK: - 배경 area 단독 (시리즈 없음 — 선택 라인 지표에 데이터가 없는 기록)

    func testBackgroundOnlyScrubShowsMarkerAndReportsInterpolatedValue() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(
            TestFixtures.emptySeries,
            backgroundArea: [AreaPoint(x: 0, y: 0), AreaPoint(x: 10, y: 100)]
        )
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        // x=5가 보간값 50이 되려면 x 도메인이 area 범위(0~10)여야 한다 —
        // 기본 layout은 시리즈가 없으면 도메인이 0~1로 붕괴하므로 이 단언이 도메인 처리까지 검증.
        view.showTouchMarker(atX: 5)
        XCTAssertTrue((view.layer.sublayers ?? []).contains { $0.name == "touch.marker" })
        XCTAssertEqual(spy.scrubbed.count, 1)
        XCTAssertEqual(spy.scrubbed.last, [:], "라인 시리즈가 없으니 시리즈 값은 빈 딕셔너리")
        XCTAssertEqual(spy.backgroundValues.last!, 50, accuracy: 1e-9)
        view.hideTouchMarker()
        XCTAssertEqual(spy.endCount, 1)
    }

    func testBackgroundOnlyScrubFollowsRawXWithoutSnapping() {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(
            TestFixtures.emptySeries,
            backgroundArea: [AreaPoint(x: 0, y: 0), AreaPoint(x: 10, y: 100)]
        )
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 2.5)  // 스냅 격자(시리즈 포인트)가 없으므로 연속 보간
        XCTAssertEqual(spy.backgroundValues.last!, 25, accuracy: 1e-9)
        view.showTouchMarker(atX: 9.9)
        XCTAssertEqual(spy.backgroundValues.last!, 99, accuracy: 1e-9)
    }

    func testBackgroundOnlyWithoutAreaMakesNoMarker() {
        // 시리즈도 area도 없으면 기존대로 아무 마커·콜백 없음.
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.render(TestFixtures.emptySeries)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 0.5)
        XCTAssertFalse((view.layer.sublayers ?? []).contains { $0.name == "touch.marker" })
        XCTAssertEqual(spy.scrubbed.count, 0)
        XCTAssertTrue(spy.backgroundValues.isEmpty)
    }

    func testSeriesSnapFailureDoesNotFallBackToBackgroundMarker() {
        // 시리즈가 있는 차트에서 확대 창 경계 스냅 실패 시, area가 있어도
        // 배경 단독 마커로 폴백하지 않는다(스냅 계약 유지 — 기존 무마커 동작 보존).
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.isZoomEnabled = true
        view.render(
            TestFixtures.fullChart,
            invertedAxes: [.primary],
            labelFormatter: TestFixtures.format,
            backgroundArea: [AreaPoint(x: 0, y: 0), AreaPoint(x: 5, y: 100)]
        )
        view.layoutIfNeeded()
        view.zoom(toXRange: 1.6 ... 1.9)
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 1.7)  // 창 안에 스냅할 점 없음(갭 줌)
        XCTAssertEqual(spy.scrubbed.count, 0)
        XCTAssertTrue(spy.backgroundValues.isEmpty)
    }

    func testBackgroundOnlyZoomResetKeepsAreaXDomain() {
        // 줌 해제(1x 복귀)의 layout 재계산 경로도 area 도메인을 유지해야 한다.
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.isAnimationEnabled = false
        view.isZoomEnabled = true
        view.render(
            TestFixtures.emptySeries,
            backgroundArea: [AreaPoint(x: 0, y: 0), AreaPoint(x: 10, y: 100)]
        )
        view.layoutIfNeeded()
        view.zoom(toXRange: 2.0 ... 6.0)
        view.layoutIfNeeded()
        view.resetZoom()
        view.layoutIfNeeded()
        let spy = SpyScrubDelegate()
        view.scrubDelegate = spy
        view.showTouchMarker(atX: 5)
        XCTAssertEqual(spy.backgroundValues.last!, 50, accuracy: 1e-9)
    }
}
