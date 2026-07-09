import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class CoreSmokeTests: XCTestCase {
    func testEngineProducesLayoutForSimpleSeries() {
        let data = LineChartData(
            series: [
                Series(
                    id: "pace",
                    points: [Point(x: 0, y: 5), Point(x: 1, y: 6)],
                    axis: .primary,
                    role: .main
                )
            ],
            referenceLines: [],
            referenceBands: [],
            segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
        let layout = LineChartEngine.shared.layout(data: data)
        XCTAssertEqual(layout.series.count, 1)
        XCTAssertFalse(layout.axisTicks.isEmpty)
        // 최신 계약(커밋 6fb6841) 확인 — stale xcframework면 컴파일 실패
        XCTAssertNil(layout.stats.segmentSeriesId)
    }

    func testBarEngineTypesExposed() {
        let data = BarChartData(
            samples: [SplitSample(distanceMeters: 1000, timeSeconds: 300)],
            splitDistanceMeters: 1000,
            targetPaceSecPerUnit: nil,
            toleranceSecPerUnit: 10,
            maxTicks: 5,
            splitTimeSeconds: nil,
            totalDurationSeconds: nil,
            totalDistanceMeters: nil
        )
        let layout = BarChartEngine.shared.layout(data: data)
        XCTAssertEqual(layout.bars.count, 1)
        // 1km 정확한 샘플(0.3km 미만 부분 구간이 아님)이므로 isPartial == false
        XCTAssertFalse(layout.bars[0].isPartial)
    }

    func testDonutEngineTypesExposed() {
        let data = DonutChartData(segments: [
            DonutSegment(value: 30, colorRole: .zone1),
            DonutSegment(value: 70, colorRole: .zone2),
        ])
        let layout = DonutEngine.shared.layout(data: data)
        XCTAssertEqual(layout.segments.count, 2)
        XCTAssertEqual(layout.total, 100, accuracy: 1e-6)
        XCTAssertEqual(layout.segments[0].sweepFraction, 0.3, accuracy: 1e-6)
    }

    func testOverlaySeriesRoleExposed() {
        let s = Series(
            id: "o",
            points: [Point(x: 0, y: 1000), Point(x: 1, y: 2000)],
            axis: .primary,
            role: .overlay
        )
        XCTAssertEqual(s.role, SeriesRole.overlay)
    }

    func testInterpolatedYExposed() {
        // 0.9.0 이관 계약 확인 — stale xcframework면 컴파일 실패.
        let points = [Point(x: 0, y: 0), Point(x: 5, y: 100)]
        let mid = LineChartEngine.shared.interpolatedY(points: points, x: 2.5)
        XCTAssertEqual(mid!.doubleValue, 50, accuracy: 1e-9)
        XCTAssertEqual(
            LineChartEngine.shared.interpolatedY(points: points, x: -1)!.doubleValue, 0,
            "범위 밖은 끝값 클램프")
        XCTAssertNil(LineChartEngine.shared.interpolatedY(points: [], x: 1))
    }
}
