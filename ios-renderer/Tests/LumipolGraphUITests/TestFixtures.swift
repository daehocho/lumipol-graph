import LumipolGraph

/// 스냅샷·터치 테스트 공용 A+C 데이터 (x = km, 0.0~5.0, 0.5 간격 11점).
enum TestFixtures {
    static let paceValues: [Double] = [6.1, 5.9, 5.75, 5.6, 5.7, 5.5, 5.35, 5.45, 5.3, 5.2, 5.4]
    static let heartRateValues: [Double] = [148, 152, 157, 160, 163, 166, 168, 170, 172, 174, 171]
    static let ghostPaceValues: [Double] = [6.4, 6.2, 6.15, 6.0, 6.05, 5.9, 5.8, 5.85, 5.7, 5.65, 5.75]

    static func series(id: String, values: [Double], axis: Axis, role: SeriesRole) -> Series {
        let points = values.enumerated().map { Point(x: Double($0.offset) * 0.5, y: $0.element) }
        return Series(id: id, points: points, axis: axis, role: role)
    }

    static var kmMarkers: [Marker] {
        (1...5).map { Marker(x: Double($0), label: "\($0)km", emphasis: $0 == 5) }
    }

    /// ① 페이스 단선
    static var paceOnly: LineChartData {
        LineChartData(
            series: [series(id: "pace", values: paceValues, axis: .primary, role: .main)],
            referenceLines: [], referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
    }

    /// ② 페이스+심박 이중축 + km 마커
    static var paceAndHeartRate: LineChartData {
        LineChartData(
            series: [
                series(id: "pace", values: paceValues, axis: .primary, role: .main),
                series(id: "hr", values: heartRateValues, axis: .secondary, role: .main),
            ],
            referenceLines: [], referenceBands: [],
            segmentMarkers: kmMarkers,
            config: ChartConfig(segmentCount: 5, maxTicks: 5)
        )
    }

    /// ③④ A+C 풀 구성: 이중축 + 고스트 + 목표선 + 목표 밴드 + km 마커
    static var fullChart: LineChartData {
        LineChartData(
            series: [
                series(id: "pace", values: paceValues, axis: .primary, role: .main),
                series(id: "pace_prev", values: ghostPaceValues, axis: .primary, role: .ghost),
                series(id: "hr", values: heartRateValues, axis: .secondary, role: .main),
            ],
            referenceLines: [RefLine(value: 5.5, axis: .primary, label: "목표 5'30\"")],
            referenceBands: [RefBand(lower: 5.4, upper: 5.6, axis: .primary)],
            segmentMarkers: kmMarkers,
            config: ChartConfig(segmentCount: 5, maxTicks: 5)
        )
    }

    /// ⑤ 시리즈 없음 — 선택 라인 지표에 데이터가 없고 배경 area(고도)만 있는 기록.
    static var emptySeries: LineChartData {
        LineChartData(
            series: [], referenceLines: [], referenceBands: [], segmentMarkers: [],
            config: ChartConfig(segmentCount: 0, maxTicks: 5)
        )
    }

    /// 페이스 mm'ss", 심박 정수, X는 km — 앱 포매터 주입 규약(스펙 결정 #5)의 테스트 구현.
    static func format(_ axis: ChartAxis, _ value: Double) -> String {
        if axis == .yPrimary {
            let minutes = Int(value)
            let seconds = Int(((value - Double(minutes)) * 60).rounded())
            return String(format: "%d'%02d\"", minutes, seconds)
        }
        if axis == .ySecondary { return "\(Int(value))" }
        return String(format: "%gkm", value)
    }
}
