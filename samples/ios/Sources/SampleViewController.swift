import UIKit
import LumipolGraphUI

/// A 데모: 페이스(반전 축)+심박 이중축 + 목표 밴드 + km 마커 + 터치 스크럽 마커.
final class SampleViewController: UIViewController {
    private let chartView = RDChartView()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        let title = UILabel()
        title.text = "이번 러닝 · 페이스 vs 심박"
        title.font = .boldSystemFont(ofSize: 17)

        title.translatesAutoresizingMaskIntoConstraints = false
        chartView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(title)
        view.addSubview(chartView)
        NSLayoutConstraint.activate([
            title.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 24),
            title.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            chartView.topAnchor.constraint(equalTo: title.bottomAnchor, constant: 16),
            chartView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 8),
            chartView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -8),
            chartView.heightAnchor.constraint(equalToConstant: 320),
        ])

        chartView.render(
            Self.sampleData(),
            invertedAxes: [.primary],
            labelFormatter: Self.format
        )
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // 데모: 등장 애니메이션 후 2.5km 지점 말풍선 표시 (터치로도 이동 가능)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [chartView] in
            chartView.showTouchMarker(atX: 2.5)
        }
    }

    private static func sampleData() -> LineChartData {
        let paceValues: [Double] = [6.1, 5.9, 5.75, 5.6, 5.7, 5.5, 5.35, 5.45, 5.3, 5.2, 5.4]
        let heartRateValues: [Double] = [148, 152, 157, 160, 163, 166, 168, 170, 172, 174, 171]
        func series(_ id: String, _ values: [Double], axis: Axis, role: SeriesRole) -> Series {
            Series(
                id: id,
                points: values.enumerated().map { Point(x: Double($0.offset) * 0.5, y: $0.element) },
                axis: axis,
                role: role
            )
        }
        return LineChartData(
            series: [
                series("pace", paceValues, axis: .primary, role: .main),
                series("hr", heartRateValues, axis: .secondary, role: .main),
            ],
            referenceBands: [RefBand(lower: 5.4, upper: 5.6, axis: .primary)],
            segmentMarkers: (1...5).map { Marker(x: Double($0), label: "\($0)km", emphasis: $0 == 5) },
            config: ChartConfig(segmentCount: 5, maxTicks: 5)
        )
    }

    private static func format(_ axis: ChartAxis, _ value: Double) -> String {
        if axis == .yPrimary {
            let minutes = Int(value)
            let seconds = Int(((value - Double(minutes)) * 60).rounded())
            return String(format: "%d'%02d\"", minutes, seconds)
        }
        if axis == .ySecondary { return "\(Int(value))" }
        return String(format: "%gkm", value)
    }
}
