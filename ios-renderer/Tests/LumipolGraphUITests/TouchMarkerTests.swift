import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class TouchMarkerTests: XCTestCase {
    // fullChart를 엔진에 통과시킨 실제 레이아웃으로 마커를 만든다.
    private let data = TestFixtures.fullChart
    private var layout: LineChartLayout { LineChartEngine.shared.layout(data: data) }
    private let plotArea = PlotArea(
        bounds: CGRect(x: 0, y: 0, width: 390, height: 300),
        insets: ChartStyle.default.plotInsets,
        invertedAxes: [.primary]
    )

    private func makeResult(atRawX rawX: Double) -> TouchMarker.Result? {
        TouchMarker.make(atRawX: rawX, context: TouchMarker.Context(
            data: data, layout: layout, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
    }

    func testMarkerContainsLineAndDotsButNoBubble() {
        let result = makeResult(atRawX: 2.4)
        XCTAssertEqual(result?.layer.name, "touch.marker")
        let names = result?.layer.sublayers?.compactMap(\.name) ?? []
        XCTAssertTrue(names.contains("touch.line"))
        XCTAssertTrue(names.contains("touch.dot.pace"))
        XCTAssertTrue(names.contains("touch.dot.hr"))
        XCTAssertFalse(names.contains("touch.bubble"), "말풍선은 제거됨")
    }

    func testReturnsFormattedValuesPerSeries() {
        // rawX 2.4 → 근접점 x=2.5 (인덱스 5): pace 5.5 → "5'30\"", hr 166 → "166"
        let values = makeResult(atRawX: 2.4)?.valuesBySeriesId ?? [:]
        XCTAssertEqual(values["pace"], "5'30\"")
        XCTAssertEqual(values["hr"], "166")
    }

    func testDotSitsOnSnappedX() {
        let result = makeResult(atRawX: 0.0)
        let line = result?.layer.sublayers?.first { $0.name == "touch.line" } as? CAShapeLayer
        let dot = result?.layer.sublayers?.first { $0.name == "touch.dot.pace" } as? CAShapeLayer
        XCTAssertNotNil(line?.path)
        // 점과 수직선의 x가 일치(스냅된 x)
        XCTAssertEqual(dot!.path!.boundingBox.midX, line!.path!.boundingBox.midX, accuracy: 0.5)
    }

    func testMarkerShownAtDomainUpperBound() {
        // 도메인 상한(마지막 포인트 x=5.0) — 정규화 x가 반올림으로 1을 수 ulp 넘어도
        // 마커가 침묵 드롭되지 않아야 한다 (경계 클램프 회귀 방지).
        let result = makeResult(atRawX: 5.0)
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.layer.name, "touch.marker")
    }

    func testOverlaySeriesValueUsesRealValueAndOverlayAxis() {
        // 원본 데이터: overlay 시리즈 실값 1500 (정규화 아님)
        let overlayData = LineChartData(
            series: [
                TestFixtures.series(id: "pace", values: TestFixtures.paceValues, axis: .primary, role: .main),
                Series(
                    id: "o",
                    points: [Point(x: 0, y: 1500), Point(x: 5, y: 1500)],
                    axis: .primary, role: .overlay
                ),
            ],
            referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
        let overlayLayout = LineChartEngine.shared.layout(data: overlayData)
        var seenOverlayAxis = false
        let result = TouchMarker.make(atRawX: 0.0, context: TouchMarker.Context(
            data: overlayData, layout: overlayLayout, style: .default, plotArea: plotArea,
            formatter: { axis, value in
                if axis == .yOverlay {
                    seenOverlayAxis = true
                    return "OV:\(Int(value))"
                }
                return TestFixtures.format(axis, value)
            }
        ))
        XCTAssertEqual(result?.valuesBySeriesId["o"], "OV:1500")
        XCTAssertTrue(seenOverlayAxis)
        let names = result?.layer.sublayers?.compactMap(\.name) ?? []
        XCTAssertFalse(names.contains("touch.dot.o"), "오버레이는 터치 점을 그리지 않음")
    }

    func testZoomedEdgeSnapsToInWindowPointWhenGlobalNearestIsOutsideWindow() {
        // 창 [0,5], main에 4.4(안)·5.3(밖): 오른쪽 끝 스크럽(rawX=5)의 전역 최근접은 5.3이지만
        // 창 밖이라며 마커 전체를 버리면 안 되고, 창 안 4.4에 스냅해야 한다(창 인식 nearest).
        let d = LineChartData(
            series: [
                Series(
                    id: "pace",
                    points: [Point(x: 0.0, y: 6.0), Point(x: 4.4, y: 5.5), Point(x: 5.3, y: 5.2)],
                    axis: .primary, role: .main
                ),
            ],
            referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
        let windowed = LineChartEngine.shared.layout(data: d, xMin: 0.0, xMax: 5.0)
        let result = TouchMarker.make(atRawX: 5.0, context: TouchMarker.Context(
            data: d, layout: windowed, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
        XCTAssertNotNil(result, "창 안 점이 있으면 마커가 표시되어야 함")
        XCTAssertEqual(result?.snappedX ?? -1, 4.4, accuracy: 1e-9)
    }

    func testSnappedXPrefersMainSeriesWhenOverlayListedFirst() {
        // 시리즈 순서 계약이 없고 오버레이는 성긴 샘플링일 수 있다 — 수직선/배경 보간의 기준인
        // snappedX는 배열 첫 시리즈가 아니라 main 시리즈의 근접점을 따라야 한다.
        let overlayFirst = LineChartData(
            series: [
                Series(
                    id: "prev",
                    points: [Point(x: 0, y: 6.0), Point(x: 2, y: 6.2)],  // 2km 간격 성긴 오버레이
                    axis: .primary, role: .overlay
                ),
                TestFixtures.series(id: "pace", values: TestFixtures.paceValues, axis: .primary, role: .main),
            ],
            referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
        let overlayFirstLayout = LineChartEngine.shared.layout(data: overlayFirst)
        let result = TouchMarker.make(atRawX: 1.3, context: TouchMarker.Context(
            data: overlayFirst, layout: overlayFirstLayout, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
        // main(0.5 간격) 근접점 = 1.5. 오버레이 근접점(2.0)이 기준이 되면 안 된다.
        XCTAssertEqual(result?.snappedX ?? -1, 1.5, accuracy: 1e-9)
    }

    func testOutOfWindowSeriesOmittedFromDotsAndValues() {
        // 확대 창(3~5)에 포인트가 전혀 없는 짧은 시리즈(0~2) — 근접점이 창 밖(x=2)으로 스냅되면
        // 창 기준 y-도메인을 벗어난 위치에 점이 그려지고 값도 스크럽 위치인 양 전달된다.
        // 창 밖 근접점은 점·값 모두 생략해야 한다.
        let shortSeries = LineChartData(
            series: [
                TestFixtures.series(id: "pace", values: TestFixtures.paceValues, axis: .primary, role: .main),
                Series(
                    id: "prev",
                    points: [Point(x: 0, y: 6.0), Point(x: 2, y: 6.2)],
                    axis: .primary, role: .main
                ),
            ],
            referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
        let windowed = LineChartEngine.shared.layout(data: shortSeries, xMin: 3.0, xMax: 5.0)
        let result = TouchMarker.make(atRawX: 4.0, context: TouchMarker.Context(
            data: shortSeries, layout: windowed, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
        XCTAssertNotNil(result)
        XCTAssertNotNil(result?.valuesBySeriesId["pace"])
        XCTAssertNil(result?.valuesBySeriesId["prev"], "창 밖 근접점 값은 전달하지 않음")
        let names = result?.layer.sublayers?.compactMap(\.name) ?? []
        XCTAssertFalse(names.contains("touch.dot.prev"), "창 밖 근접점에는 점을 그리지 않음")
    }

    func testDuplicateSeriesIdsDoNotCrash() {
        // 코어 API는 시리즈 id 유일성을 강제하지 않는다 — 중복 id가 fatalError로
        // 이어지지 않고 첫 시리즈 기준으로 마커를 만들어야 한다.
        let dupData = LineChartData(
            series: [
                TestFixtures.series(id: "pace", values: TestFixtures.paceValues, axis: .primary, role: .main),
                TestFixtures.series(id: "pace", values: TestFixtures.altPaceValues, axis: .primary, role: .main),
            ],
            referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
        let dupLayout = LineChartEngine.shared.layout(data: dupData)
        let result = TouchMarker.make(atRawX: 2.4, context: TouchMarker.Context(
            data: dupData, layout: dupLayout, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
        XCTAssertNotNil(result)
    }

    func testReturnsNilWhenAxisScaleUnavailable() {
        // 축 tick이 없는 빈 레이아웃 → 역산 불능 → nil
        let emptyLayout = LineChartLayout(
            series: [], axisTicks: [], refBands: [], markers: [],
            stats: Stats(perSeries: [], segments: [], segmentSeriesId: nil)
        )
        let result = TouchMarker.make(atRawX: 1.0, context: TouchMarker.Context(
            data: data, layout: emptyLayout, style: .default, plotArea: plotArea,
            formatter: TestFixtures.format
        ))
        XCTAssertNil(result)
    }
}
