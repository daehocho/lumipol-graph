import SnapshotTesting
import XCTest
import LumipolGraph
@testable import LumipolGraphUI

/// 결정성: 시뮬레이터 iPhone 17 Pro(iOS 26.4) 고정, 390×300, 애니메이션 off, 라이트 모드.
final class SnapshotTests: XCTestCase {
    private func makeChartView(
        _ data: LineChartData,
        invertedAxes: Set<Axis> = [],
        formatter: ((ChartAxis, Double) -> String)? = nil
    ) -> RDChartView {
        let view = RDChartView(frame: CGRect(x: 0, y: 0, width: 390, height: 300))
        view.backgroundColor = .white
        view.overrideUserInterfaceStyle = .light
        view.isAnimationEnabled = false
        view.render(data, invertedAxes: invertedAxes, labelFormatter: formatter)
        view.layoutIfNeeded()
        return view
    }

    // ① 페이스 단선 (반전 축: 위=빠름)
    func testPaceSingleLine() {
        let view = makeChartView(
            TestFixtures.paceOnly, invertedAxes: [.primary], formatter: TestFixtures.format
        )
        assertSnapshot(of: view, as: .image)
    }

    // ② 페이스+심박 이중축 + km 마커
    func testPaceAndHeartRateDualAxis() {
        let view = makeChartView(
            TestFixtures.paceAndHeartRate, invertedAxes: [.primary], formatter: TestFixtures.format
        )
        assertSnapshot(of: view, as: .image)
    }

    // ③ 고스트 + 목표선 + 목표 밴드 (A+C 풀 구성)
    func testGhostReferenceLineAndBand() {
        let view = makeChartView(
            TestFixtures.fullChart, invertedAxes: [.primary], formatter: TestFixtures.format
        )
        assertSnapshot(of: view, as: .image)
    }

    // ④ 터치 마커 표시 (x=2.4 → 2.5km 지점 스냅, 세로선+점만 · 말풍선 없음)
    func testTouchMarkerShown() {
        let view = makeChartView(
            TestFixtures.fullChart, invertedAxes: [.primary], formatter: TestFixtures.format
        )
        view.showTouchMarker(atX: 2.4)
        assertSnapshot(of: view, as: .image)
    }

    // ⑤ 확대 상태 (1.0~3.0km 창) — Y 재계산·tick 윈도잉·클립 확인
    func testZoomedWindow() {
        let view = makeChartView(
            TestFixtures.fullChart, invertedAxes: [.primary], formatter: TestFixtures.format
        )
        view.isZoomEnabled = true
        view.zoom(toXRange: 1.0...3.0)
        assertSnapshot(of: view, as: .image)
    }

    // ⑥ 막대 차트 — 롱프레스 선택 상태(가운데 막대: dim + 가이드선 + 페이스 말풍선)
    func testBarChartSelectedState() {
        let view = RDBarChartView(frame: CGRect(x: 0, y: 0, width: 320, height: 220))
        view.backgroundColor = .white
        view.overrideUserInterfaceStyle = .light
        func role(for i: Int) -> BarColorRole {
            if i < 3 { return .slower }
            if i < 6 { return .onTarget }
            return .faster
        }
        var bars: [BarLayout] = []
        for i in 0..<8 {
            let value: Double = 300 + Double(i) * 5
            let heightFraction: Double = 0.3 + 0.07 * Double(i)
            bars.append(BarLayout(index: Int32(i), value: value, heightFraction: heightFraction,
                                   colorRole: role(for: i), isPartial: false, endMinutes: nil))
        }
        let layout = BarChartLayout(bars: bars, yTicks: [], referenceLinePosition: KotlinDouble(double: 0.5))
        view.render(layout, style: .default,
                    barLabels: bars.map { _ in "5'00\"" }, xAxisLabels: nil, yLabelFormatter: nil)
        view.layoutIfNeeded()
        view.selectBar(at: 4)
        assertSnapshot(of: view, as: .image)
    }
}
