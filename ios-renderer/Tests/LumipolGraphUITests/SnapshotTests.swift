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

    // ④ 터치 마커 표시 (x=2.4 → 2.5km 지점 스냅, 페이스·심박 동시 말풍선)
    func testTouchMarkerShown() {
        let view = makeChartView(
            TestFixtures.fullChart, invertedAxes: [.primary], formatter: TestFixtures.format
        )
        view.showTouchMarker(atX: 2.4)
        assertSnapshot(of: view, as: .image)
    }
}
