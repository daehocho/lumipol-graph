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

    func testTouchesCancelledSendsDeselect() {
        // 스크롤뷰 팬·시스템 제스처가 터치를 가로채면 touchesEnded 대신 touchesCancelled가 온다 —
        // 이때도 해제(nil)를 보내야 호스트의 존 하이라이트가 고착되지 않는다.
        let view = makeView()
        view.render(DonutChartData(segments: [DonutSegment(value: 100, colorRole: .zone2)]))
        view.layoutIfNeeded()
        let spy = SpyZoneDelegate()
        view.zoneDelegate = spy
        view.touchesCancelled(Set<UITouch>(), with: nil)
        XCTAssertEqual(spy.selections, [nil], "취소 시 해제(nil) 콜백 1회")
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
}
