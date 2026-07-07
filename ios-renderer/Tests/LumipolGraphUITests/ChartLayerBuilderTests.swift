import XCTest
import LumipolGraph
@testable import LumipolGraphUI

final class ChartLayerBuilderTests: XCTestCase {
    // 손으로 만든 정규화 레이아웃 — plotArea rect (0,0,100,100)에서 픽셀 정확값을 검증한다.
    private let layout = LineChartLayout(
        series: [
            SeriesLayout(id: "pace", role: .main, points: [
                NormalizedPoint(x: 0, y: 0), NormalizedPoint(x: 1, y: 1),
            ]),
            SeriesLayout(id: "pace_prev", role: .ghost, points: [
                NormalizedPoint(x: 0, y: 1), NormalizedPoint(x: 1, y: 0),
            ]),
        ],
        axisTicks: [
            AxisTicksLayout(axis: .x, ticks: [AxisTick(value: 0, position: 0), AxisTick(value: 5, position: 1)]),
            AxisTicksLayout(axis: .yPrimary, ticks: [AxisTick(value: 4, position: 0), AxisTick(value: 6, position: 1)]),
        ],
        refLines: [RefLineLayout(axis: .primary, position: 0.5, label: "목표")],
        refBands: [RefBandLayout(axis: .primary, lower: 0.25, upper: 0.75)],
        markers: [
            MarkerLayout(position: 0.5, label: "1km", emphasis: false),
            MarkerLayout(position: 1.0, label: "2km", emphasis: true),
        ],
        stats: Stats(perSeries: [], segments: [], segmentSeriesId: nil)
    )
    private let data = LineChartData(
        series: [
            Series(id: "pace", points: [], axis: .primary, role: .main),
            Series(id: "pace_prev", points: [], axis: .primary, role: .ghost),
        ],
        referenceLines: [], referenceBands: [], segmentMarkers: [],
        config: ChartConfig(segmentCount: 0, maxTicks: 5)
    )
    private let plotArea = PlotArea(bounds: CGRect(x: 0, y: 0, width: 100, height: 100), insets: .zero)

    private func build() -> [CALayer] {
        ChartLayerBuilder.build(
            layout: layout, data: data, style: .default, plotArea: plotArea,
            formatter: { _, value in "\(value)" }
        )
    }

    private func layer(named name: String, in layers: [CALayer]) -> CALayer? {
        layers.first { $0.name == name }
    }

    /// CGPath의 move/line 포인트를 순서대로 추출(테스트 전용 — 곡선 세그먼트는 이 스위트에서 쓰지 않음).
    private func pathPoints(_ path: CGPath) -> [CGPoint] {
        var points: [CGPoint] = []
        path.applyWithBlock { elementPtr in
            let element = elementPtr.pointee
            switch element.type {
            case .moveToPoint, .addLineToPoint:
                points.append(element.points[0])
            default:
                break
            }
        }
        return points
    }

    func testBuildsExpectedLayerTree() {
        let names = build().compactMap(\.name)
        XCTAssertEqual(names, [
            "grid",
            "band.0", "marker.0", "marker.1",
            "series.ghost.pace_prev",
            "series.gradient.pace", "series.main.pace",
            "refLine.0",
            "axisLabels.x", "axisLabels.yPrimary",
        ])
    }

    func testGridDrawsDashedLinesAtTicks() {
        let grid = layer(named: "grid", in: build()) as? CAShapeLayer
        XCTAssertEqual(grid?.lineDashPattern, ChartStyle.default.gridLineDashPattern)
        // x tick 2개(0,1) 세로선 + yPrimary tick 2개(0,1) 가로선 → 플롯 전체 바운딩
        XCTAssertEqual(grid?.path?.boundingBox, CGRect(x: 0, y: 0, width: 100, height: 100))
    }

    func testNilGridColorOmitsGridLayer() {
        var style = ChartStyle.default
        style.gridLineColor = nil
        let layers = ChartLayerBuilder.build(
            layout: layout, data: data, style: style, plotArea: plotArea,
            formatter: { _, value in "\(value)" }
        )
        XCTAssertNil(layer(named: "grid", in: layers))
    }

    func testGradientOnlyOnPrimaryAxisSeries() {
        // secondary 축 main 시리즈는 fill 중첩 방지를 위해 그라데이션 없이 라인만 그린다.
        let dualLayout = LineChartLayout(
            series: [
                SeriesLayout(id: "pace", role: .main, points: [
                    NormalizedPoint(x: 0, y: 0), NormalizedPoint(x: 1, y: 1),
                ]),
                SeriesLayout(id: "hr", role: .main, points: [
                    NormalizedPoint(x: 0, y: 1), NormalizedPoint(x: 1, y: 0),
                ]),
            ],
            axisTicks: [], refLines: [], refBands: [], markers: [],
            stats: Stats(perSeries: [], segments: [], segmentSeriesId: nil)
        )
        let dualData = LineChartData(
            series: [
                Series(id: "pace", points: [], axis: .primary, role: .main),
                Series(id: "hr", points: [], axis: .secondary, role: .main),
            ],
            referenceLines: [], referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
        let layers = ChartLayerBuilder.build(
            layout: dualLayout, data: dualData, style: .default, plotArea: plotArea,
            formatter: { _, value in "\(value)" }
        )
        XCTAssertNotNil(layer(named: "series.gradient.pace", in: layers))
        XCTAssertNil(layer(named: "series.gradient.hr", in: layers))
        XCTAssertNotNil(layer(named: "series.main.hr", in: layers))
    }

    func testMainLinePathSpansPlotRect() {
        let main = layer(named: "series.main.pace", in: build()) as? CAShapeLayer
        // (0,0)→아래왼쪽 (0,100), (1,1)→위오른쪽 (100,0)
        XCTAssertEqual(main?.path?.boundingBox, CGRect(x: 0, y: 0, width: 100, height: 100))
        XCTAssertEqual(main?.lineWidth, ChartStyle.default.lineWidth)
    }

    func testGhostLineIsDashed() {
        let ghost = layer(named: "series.ghost.pace_prev", in: build()) as? CAShapeLayer
        XCTAssertEqual(ghost?.lineDashPattern, ChartStyle.default.ghostDashPattern)
    }

    func testRefLineSitsAtNormalizedPosition() {
        let refLine = layer(named: "refLine.0", in: build())
        let line = refLine?.sublayers?.compactMap { $0 as? CAShapeLayer }.first
        // position 0.5, 정상 축 → y = 50
        XCTAssertEqual(line?.path?.boundingBox.midY, 50)
        // 라벨(CATextLayer) 존재
        XCTAssertTrue(refLine?.sublayers?.contains { $0 is CATextLayer } ?? false)
    }

    func testBandCoversNormalizedRange() {
        let band = layer(named: "band.0", in: build())
        // lower 0.25→y75, upper 0.75→y25 (정상 축) → frame (0,25,100,50)
        XCTAssertEqual(band?.frame, CGRect(x: 0, y: 25, width: 100, height: 50))
    }

    func testMarkersAreVerticalLinesWithLabels() {
        let layers = build()
        let marker = layer(named: "marker.0", in: layers)
        let line = marker?.sublayers?.compactMap { $0 as? CAShapeLayer }.first
        XCTAssertEqual(line?.path?.boundingBox.midX, 50)
        XCTAssertTrue(marker?.sublayers?.contains { $0 is CATextLayer } ?? false)
        // emphasis 마커는 굵은 선
        let emphasisLine = layer(named: "marker.1", in: layers)?
            .sublayers?.compactMap { $0 as? CAShapeLayer }.first
        XCTAssertEqual(emphasisLine?.lineWidth, 1.5)
    }

    func testAxisLabelsUseFormatter() {
        let layers = ChartLayerBuilder.build(
            layout: layout, data: data, style: .default, plotArea: plotArea,
            formatter: { axis, value in axis == .x ? "\(Int(value))km" : "v\(Int(value))" }
        )
        let xLabels = layer(named: "axisLabels.x", in: layers)?
            .sublayers?.compactMap { ($0 as? CATextLayer)?.string as? String }
        XCTAssertEqual(xLabels, ["0km", "5km"])
        let yLabels = layer(named: "axisLabels.yPrimary", in: layers)?
            .sublayers?.compactMap { ($0 as? CATextLayer)?.string as? String }
        XCTAssertEqual(yLabels, ["v4", "v6"])
    }

    func testInvertedAxisFlipsSeriesAndRefLine() {
        let inverted = PlotArea(
            bounds: CGRect(x: 0, y: 0, width: 100, height: 100), insets: .zero,
            invertedAxes: [.primary]
        )
        let layers = ChartLayerBuilder.build(
            layout: layout, data: data, style: .default, plotArea: inverted,
            formatter: { _, value in "\(value)" }
        )
        let band = layer(named: "band.0", in: layers)
        // 반전이어도 min/max 처리로 동일 프레임
        XCTAssertEqual(band?.frame, CGRect(x: 0, y: 25, width: 100, height: 50))
    }

    func testNotRenderablePlotAreaProducesNoLayers() {
        let zero = PlotArea(bounds: .zero, insets: .zero)
        let layers = ChartLayerBuilder.build(
            layout: layout, data: data, style: .default, plotArea: zero,
            formatter: { _, value in "\(value)" }
        )
        XCTAssertTrue(layers.isEmpty)
    }

    func testOverlaySeriesProducesDashedLayerWithoutAxisLabelsOrGradient() {
        let overlayLayout = LineChartLayout(
            series: [
                SeriesLayout(id: "p", role: .main, points: [
                    NormalizedPoint(x: 0, y: 0.2), NormalizedPoint(x: 1, y: 0.8),
                ]),
                SeriesLayout(id: "o", role: .overlay, points: [
                    NormalizedPoint(x: 0, y: 0.0), NormalizedPoint(x: 1, y: 1.0),
                ]),
            ],
            axisTicks: [], refLines: [], refBands: [], markers: [],
            stats: Stats(perSeries: [], segments: [], segmentSeriesId: nil)
        )
        let overlayData = LineChartData(
            series: [
                Series(id: "p", points: [], axis: .primary, role: .main),
                Series(id: "o", points: [], axis: .primary, role: .overlay),
            ],
            referenceLines: [], referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
        let layers = ChartLayerBuilder.build(
            layout: overlayLayout, data: overlayData, style: .default, plotArea: plotArea,
            formatter: { _, value in "\(value)" }
        )
        let names = layers.compactMap(\.name)
        XCTAssertTrue(names.contains("series.overlay.o"))
        XCTAssertFalse(names.contains { $0.hasPrefix("series.gradient.o") })
        XCTAssertFalse(names.contains { $0.hasPrefix("axisLabels") })
        let overlay = layer(named: "series.overlay.o", in: layers) as? CAShapeLayer
        XCTAssertEqual(overlay?.lineDashPattern, ChartStyle.default.overlayLineDashPattern)
        XCTAssertEqual(overlay?.strokeColor, ChartStyle.default.overlayLineColor.cgColor)
        XCTAssertEqual(overlay?.lineWidth, ChartStyle.default.overlayLineWidth)
    }

    func testOverlaySeriesIgnoresHostAxisInversion() {
        // 오버레이는 코어가 자체 정규화한 값(1.0=최대)이라, 호스트 축이 반전(pace 축 등)이어도
        // 항상 "값이 클수록 위"로 그려야 한다.
        let overlayLayout = LineChartLayout(
            series: [
                SeriesLayout(id: "o", role: .overlay, points: [
                    NormalizedPoint(x: 0, y: 0.0), NormalizedPoint(x: 1, y: 1.0),
                ]),
            ],
            axisTicks: [], refLines: [], refBands: [], markers: [],
            stats: Stats(perSeries: [], segments: [], segmentSeriesId: nil)
        )
        let overlayData = LineChartData(
            series: [
                Series(id: "o", points: [], axis: .primary, role: .overlay),
            ],
            referenceLines: [], referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
        let invertedPlotArea = PlotArea(
            bounds: CGRect(x: 0, y: 0, width: 100, height: 100), insets: .zero,
            invertedAxes: [.primary]
        )
        let layers = ChartLayerBuilder.build(
            layout: overlayLayout, data: overlayData, style: .default, plotArea: invertedPlotArea,
            formatter: { _, value in "\(value)" }
        )
        let overlay = layer(named: "series.overlay.o", in: layers) as? CAShapeLayer
        let points = overlay?.path.map(pathPoints) ?? []
        XCTAssertEqual(points.count, 2)
        // x=0 → y-fraction 0.0(최소), x=1 → y-fraction 1.0(최대) → 최대값이 화면 위(작은 y)여야 함
        XCTAssertLessThan(points[1].y, points[0].y)
    }

    func testSeriesWithFewerThanTwoPointsIsSkipped() {
        let single = LineChartLayout(
            series: [SeriesLayout(id: "one", role: .main, points: [NormalizedPoint(x: 0.5, y: 0.5)])],
            axisTicks: [], refLines: [], refBands: [], markers: [],
            stats: Stats(perSeries: [], segments: [], segmentSeriesId: nil)
        )
        let layers = ChartLayerBuilder.build(
            layout: single, data: data, style: .default, plotArea: plotArea,
            formatter: { _, value in "\(value)" }
        )
        XCTAssertTrue(layers.isEmpty)
    }
}
