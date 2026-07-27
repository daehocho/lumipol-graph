import XCTest
@testable import LumipolGraphUI
import LumipolGraph

final class HeartRateZoneViewTests: XCTestCase {

    private func makeView() -> RDHeartRateZoneView {
        RDHeartRateZoneView(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
    }

    func testRendersOneLayerPerSegment() {
        let view = makeView()
        view.render(DonutChartData(segments: [
            DonutSegment(value: 30, colorRole: .zone1),
            DonutSegment(value: 40, colorRole: .zone3),
            DonutSegment(value: 30, colorRole: .zone5),
        ]))
        view.layoutIfNeeded()
        XCTAssertEqual(view.segmentLayers.count, 3)
    }

    func testEmptyDataRendersSingleGrayRing() {
        let view = makeView()
        view.render(DonutChartData(segments: []))
        view.layoutIfNeeded()
        XCTAssertEqual(view.segmentLayers.count, 1)
        XCTAssertEqual(view.segmentLayers[0].strokeColor, ChartStyle.default.donutEmptyColor.cgColor)
    }

    func testSegmentUsesStyleColor() {
        let view = makeView()
        view.render(DonutChartData(segments: [DonutSegment(value: 100, colorRole: .zone4)]))
        view.layoutIfNeeded()
        XCTAssertEqual(view.segmentLayers[0].strokeColor, ChartStyle.default.donutColors[.zone4]!.cgColor)
    }

    // MARK: - 히트 테스트 (segmentIndex)

    /// 링 위의 점: 정규화 fraction(0=12시, 시계방향) 위치의 링 중심선 좌표.
    private func ringPoint(in view: RDHeartRateZoneView, atFraction frac: CGFloat) -> CGPoint {
        let ring = ChartStyle.default.donutRingWidth
        let radius = (min(view.bounds.width, view.bounds.height) - ring) / 2
        let angle = frac * 2 * .pi - .pi / 2
        return CGPoint(
            x: view.bounds.midX + radius * cos(angle),
            y: view.bounds.midY + radius * sin(angle)
        )
    }

    func testSegmentIndexMapsToOriginalDataIndexWhenZeroValueSegmentsFiltered() {
        // zone1의 value가 0이면 DonutEngine이 레이아웃에서 제외한다 —
        // 델리게이트 인덱스는 필터된 레이아웃이 아니라 호출자가 준 data.segments 기준이어야 한다.
        let view = makeView()
        view.render(DonutChartData(segments: [
            DonutSegment(value: 0, colorRole: .zone1),
            DonutSegment(value: 30, colorRole: .zone2),
            DonutSegment(value: 70, colorRole: .zone3),
        ]))
        view.layoutIfNeeded()
        // 첫 호(0~30%) = zone2 → 원본 인덱스 1
        XCTAssertEqual(view.segmentIndex(at: ringPoint(in: view, atFraction: 0.15)), 1)
        // 두 번째 호(30~100%) = zone3 → 원본 인덱스 2
        XCTAssertEqual(view.segmentIndex(at: ringPoint(in: view, atFraction: 0.6)), 2)
    }

    func testTouchInDonutHoleOrOutsideRingSelectsNothing() {
        // 도넛 구멍 중앙(앱이 요약 텍스트를 두는 자리)이나 링 밖 모서리 탭 —
        // 각도만으로는 항상 어떤 세그먼트에 매칭되므로 반경 검사가 없으면 허위 선택이 발생한다.
        let view = makeView()
        view.render(DonutChartData(segments: [
            DonutSegment(value: 30, colorRole: .zone1),
            DonutSegment(value: 70, colorRole: .zone3),
        ]))
        view.layoutIfNeeded()
        XCTAssertNil(view.segmentIndex(at: CGPoint(x: 100, y: 105)), "구멍 중앙은 선택 없음")
        XCTAssertNil(view.segmentIndex(at: CGPoint(x: 5, y: 5)), "링 밖 모서리는 선택 없음")
        // 링 위는 여전히 선택된다 (회귀 방지)
        XCTAssertEqual(view.segmentIndex(at: ringPoint(in: view, atFraction: 0.15)), 0)
    }

    // MARK: - 터치 취소

    private final class SpyZoneDelegate: RDHeartRateZoneSelectionDelegate {
        var selections: [Int?] = []
        func heartRateZoneView(_ view: RDHeartRateZoneView, didSelectSegmentAt index: Int?) {
            selections.append(index)
        }
    }

    // MARK: - 탭 토글 선택 (0.26.0)

    /// 3-세그먼트 표준 데이터: zone2 30%(idx0) + zone3 70%(idx1).
    private func makeSelectableView(delegate: SpyZoneDelegate? = nil,
                                    style: ChartStyle = .default) -> RDHeartRateZoneView {
        let view = makeView()
        view.zoneDelegate = delegate
        view.render(DonutChartData(segments: [
            DonutSegment(value: 30, colorRole: .zone2, label: "저강도"),
            DonutSegment(value: 70, colorRole: .zone3, label: "유산소"),
        ]), style: style)
        view.layoutIfNeeded()
        return view
    }

    func testTapSelectsAndShowsCenterLabel() {
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))
        XCTAssertEqual(view.selectedIndex, 0)
        XCTAssertEqual(spy.selections, [0])
        XCTAssertEqual(view.zoneNameLabel.text, "저강도")
        XCTAssertEqual(view.percentLabel.text, "30%")
        XCTAssertFalse(view.zoneNameLabel.isHidden)
        XCTAssertFalse(view.percentLabel.isHidden)
    }

    func testTapSameSegmentTogglesOff() {
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(spy.selections, [0, nil])
        XCTAssertTrue(view.percentLabel.isHidden)
    }

    func testTapOtherSegmentMovesSelection() {
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))
        view.handleTap(at: ringPoint(in: view, atFraction: 0.6))
        XCTAssertEqual(view.selectedIndex, 1)
        XCTAssertEqual(spy.selections, [0, 1])
    }

    func testTapOutsideRingClearsSelection() {
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))
        view.handleTap(at: CGPoint(x: 100, y: 105))  // 도넛 구멍
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(spy.selections, [0, nil])
    }

    func testTapOutsideWithoutSelectionDoesNotNotify() {
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.handleTap(at: CGPoint(x: 100, y: 105))
        XCTAssertEqual(spy.selections, [], "무선택 상태의 허공 탭은 통지하지 않는다")
    }

    func testSelectionDimsUnselectedSegments() {
        let view = makeSelectableView()
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))
        let base = ChartStyle.default.donutColors[.zone3]!
        XCTAssertEqual(view.segmentLayers[1].strokeColor,
                       base.withAlphaComponent(ChartStyle.default.donutDimmedAlpha).cgColor)
        XCTAssertEqual(view.segmentLayers[0].strokeColor,
                       ChartStyle.default.donutColors[.zone2]!.cgColor, "선택 조각은 원래 색 유지")
    }

    func testNilLabelShowsPercentOnly() {
        let view = makeView()
        view.render(DonutChartData(segments: [DonutSegment(value: 100, colorRole: .zone5)]))
        view.layoutIfNeeded()
        view.handleTap(at: ringPoint(in: view, atFraction: 0.5))
        XCTAssertTrue(view.zoneNameLabel.isHidden)
        XCTAssertEqual(view.percentLabel.text, "100%")
    }

    func testRenderResetsSelectionSilently() {
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))
        view.render(DonutChartData(segments: [DonutSegment(value: 100, colorRole: .zone1)]))
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(spy.selections, [0], "render 리셋은 델리게이트 통지 없음")
        XCTAssertTrue(view.percentLabel.isHidden)
    }

    func testTouchesCancelledKeepsSelection() {
        // 토글 모델엔 "누르는 동안" 상태가 없다 — 스크롤 가로챔이 선택을 지우면 안 된다.
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))
        view.touchesCancelled(Set<UITouch>(), with: nil)
        XCTAssertEqual(view.selectedIndex, 0)
        XCTAssertEqual(spy.selections, [0])
    }

    func testAutoDeselectAfterDelay() {
        var style = ChartStyle.default
        style.donutAutoDeselectDelay = 0.05
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy, style: style)
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))
        let exp = expectation(description: "auto deselect")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { exp.fulfill() }
        wait(for: [exp], timeout: 2)
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(spy.selections, [0, nil], "타이머 만료는 해제 통지")
    }

    func testTinyBoundsSkipsDrawingInsteadOfNegativeRadius() {
        // ring width(기본 28)보다 작은 bounds → radius 가 음수가 되어 arc 생성 시 크래시/쓰레기 값 위험.
        // guard 로 조기 반환 → 세그먼트 레이어 없음, 크래시 없음.
        let view = RDHeartRateZoneView(frame: CGRect(x: 0, y: 0, width: 10, height: 10))
        view.render(DonutChartData(segments: [
            DonutSegment(value: 30, colorRole: .zone1),
            DonutSegment(value: 70, colorRole: .zone3),
        ]))
        view.layoutIfNeeded()
        XCTAssertEqual(view.segmentLayers.count, 0)
    }

    // MARK: - 외부 구동 선택 (0.27.0, 레전드 탭)

    func testSelectSegmentSelectsAndNotifies() {
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.selectSegment(at: 1)
        XCTAssertEqual(view.selectedIndex, 1)
        XCTAssertEqual(spy.selections, [1])
        XCTAssertEqual(view.zoneNameLabel.text, "유산소")
        XCTAssertEqual(view.percentLabel.text, "70%")
    }

    func testSelectSegmentSameIndexTogglesOff() {
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.selectSegment(at: 0)
        view.selectSegment(at: 0)
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(spy.selections, [0, nil])
    }

    func testSelectSegmentNilClears() {
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.selectSegment(at: 0)
        view.selectSegment(at: nil)
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(spy.selections, [0, nil])
    }

    func testSelectSegmentIgnoresIndexNotInLayout() {
        // 범위 밖·필터된(value<=0) 인덱스는 해당 호가 없으므로 무시 — 상태·통지 불변.
        let spy = SpyZoneDelegate()
        let view = makeView()
        view.zoneDelegate = spy
        view.render(DonutChartData(segments: [
            DonutSegment(value: 0, colorRole: .zone1, label: "워밍업"),
            DonutSegment(value: 100, colorRole: .zone2, label: "저강도"),
        ]))
        view.layoutIfNeeded()
        view.selectSegment(at: 0)   // 필터된 세그먼트
        view.selectSegment(at: 7)   // 범위 밖
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(spy.selections, [])
        view.selectSegment(at: 1)   // 실재하는 인덱스는 정상 동작(회귀 방지)
        XCTAssertEqual(view.selectedIndex, 1)
    }

    func testTapAndSelectSegmentShareSelection() {
        // 도넛 탭으로 고른 존을 레전드로 다시 누르면 해제된다 — 두 진입점이 같은 상태를 쓴다.
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy)
        view.handleTap(at: ringPoint(in: view, atFraction: 0.15))  // idx 0 선택
        view.selectSegment(at: 0)
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(spy.selections, [0, nil])
    }

    func testSelectSegmentStartsAutoDeselectTimer() {
        var style = ChartStyle.default
        style.donutAutoDeselectDelay = 0.05
        let spy = SpyZoneDelegate()
        let view = makeSelectableView(delegate: spy, style: style)
        view.selectSegment(at: 1)
        let exp = expectation(description: "auto deselect")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { exp.fulfill() }
        wait(for: [exp], timeout: 2)
        XCTAssertNil(view.selectedIndex)
        XCTAssertEqual(spy.selections, [1, nil])
    }
}
