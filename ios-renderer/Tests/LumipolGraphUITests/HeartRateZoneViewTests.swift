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
