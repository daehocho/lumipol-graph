import UIKit
import LumipolGraph

/// 터치 지점의 근접점 마커 레이어(수직선 + 시리즈별 점)를 만들고, 시리즈별 포맷 값을 반환한다.
/// 근접 판정은 코어 `LineChartEngine.nearest`(원본 도메인 x) — 양 플랫폼 동일 로직.
enum TouchMarker {
    struct Context {
        let data: LineChartData
        let layout: LineChartLayout
        let style: ChartStyle
        let plotArea: PlotArea
        let formatter: (ChartAxis, Double) -> String
    }

    /// 마커 레이어 + seriesId→포맷값. 값(표시명 제외)은 스크럽 콜백으로 앱에 전달된다.
    struct Result {
        let layer: CALayer
        let valuesBySeriesId: [String: String]
        /// 수직선 기준으로 스냅된 원본 도메인 x(첫 시리즈 근접점). 배경 area 보간에 사용.
        let snappedX: Double
    }

    /// 시리즈 없는 차트(배경 area 단독)용 마커 — 스냅 격자(시리즈 포인트)가 없으므로
    /// rawX를 그대로 수직선 위치로 쓴다(연속 스크럽). 시리즈 값이 없어 valuesBySeriesId는 빈 딕셔너리.
    static func makeBackgroundOnly(atRawX rawX: Double, context: Context) -> Result? {
        guard context.plotArea.isRenderable,
              let xTicks = ticks(for: .x, in: context.layout),
              let xScale = AxisScale(ticks: xTicks)
        else { return nil }
        let rawNx = xScale.position(ofValue: rawX)
        guard rawNx >= -1e-9, rawNx <= 1 + 1e-9 else { return nil }
        let nx = min(max(rawNx, 0), 1)
        let container = CALayer()
        container.name = "touch.marker"
        container.addSublayer(verticalLine(atNx: nx, context: context))
        return Result(
            layer: container, valuesBySeriesId: [:],
            snappedX: xScale.value(atPosition: nx)
        )
    }

    /// 원본 도메인 x 기준 마커. 표시 불가(플롯 없음·축 변환 불능·근접점 없음)면 nil.
    static func make(atRawX rawX: Double, context: Context) -> Result? {
        guard context.plotArea.isRenderable,
              let xTicks = ticks(for: .x, in: context.layout),
              let xScale = AxisScale(ticks: xTicks)
        else { return nil }
        let results = LineChartEngine.shared.nearest(data: context.data, x: rawX)
        // 코어 API가 시리즈 id 유일성을 강제하지 않으므로 중복 시 첫 시리즈 우선(fatalError 방지).
        let axisBySeriesId = Dictionary(
            context.data.series.map { ($0.id, $0.axis) }, uniquingKeysWith: { first, _ in first }
        )
        let roleBySeriesId = Dictionary(
            context.data.series.map { ($0.id, $0.role) }, uniquingKeysWith: { first, _ in first }
        )
        // 수직선 x는 main 시리즈의 근접점 기준 — 시리즈 순서 계약이 없고 고스트/오버레이는
        // 성긴 샘플링일 수 있어, 스냅 격자는 본 기록(main)을 따른다. main 없으면 첫 시리즈 폴백.
        let snapSource = results.first { roleBySeriesId[$0.seriesId] == .main } ?? results.first
        guard let snappedX = snapSource?.x else { return nil }
        // 스냅된 근접점이 현재 표시 도메인(창) 밖이면 마커를 만들지 않는다 —
        // 확대 창 경계 부근에서 창 밖 데이터로 스냅되는 것을 방지.
        // 단, 도메인 양끝 값은 부동소수점 반올림으로 0/1을 수 ulp 벗어날 수 있으므로
        // epsilon 이내는 창 안으로 간주해 클램프한다 (엄격 비교 시 끝 탭이 침묵 드롭됨).
        let rawNx = xScale.position(ofValue: snappedX)
        guard rawNx >= -1e-9, rawNx <= 1 + 1e-9 else { return nil }
        let nx = min(max(rawNx, 0), 1)

        let container = CALayer()
        container.name = "touch.marker"
        container.addSublayer(verticalLine(atNx: nx, context: context))

        var valuesBySeriesId: [String: String] = [:]
        for result in results {
            // 시리즈별 근접점이 현재 표시 도메인(창) 밖이면 점·값 모두 생략 —
            // 창에 포인트가 없는 시리즈(예: 짧은 고스트)가 창 밖 값을 스크럽 위치인 양
            // 보고하거나, 창 기준 y-도메인을 벗어난 위치에 점을 그리는 것을 방지.
            let seriesNx = xScale.position(ofValue: result.x)
            guard seriesNx >= -1e-9, seriesNx <= 1 + 1e-9 else { continue }
            if roleBySeriesId[result.seriesId] == .overlay {
                // 오버레이: 축 없음 — 실값만 툴팁에 표시(터치 점은 생략).
                valuesBySeriesId[result.seriesId] = context.formatter(.yOverlay, result.y)
                continue
            }
            guard let axis = axisBySeriesId[result.seriesId],
                  let chartAxis = chartAxis(of: axis),
                  let yTicks = ticks(for: chartAxis, in: context.layout),
                  let yScale = AxisScale(ticks: yTicks)
            else { continue }
            let point = context.plotArea.point(
                NormalizedPoint(x: nx, y: yScale.position(ofValue: result.y)),
                axis: axis
            )
            let dot = CAShapeLayer()
            dot.name = "touch.dot.\(result.seriesId)"
            dot.path = UIBezierPath(
                arcCenter: point, radius: context.style.touchDotRadius,
                startAngle: 0, endAngle: .pi * 2, clockwise: true
            ).cgPath
            let dotColor: UIColor
            if roleBySeriesId[result.seriesId] == .ghost {
                dotColor = context.style.ghostLineColor
            } else if axis == .secondary {
                dotColor = context.style.secondaryLineColor
            } else {
                dotColor = context.style.primaryLineColor
            }
            dot.fillColor = dotColor.cgColor
            container.addSublayer(dot)
            valuesBySeriesId[result.seriesId] = context.formatter(chartAxis, result.y)
        }
        guard !valuesBySeriesId.isEmpty else { return nil }
        return Result(layer: container, valuesBySeriesId: valuesBySeriesId, snappedX: snappedX)
    }

    private static func verticalLine(atNx nx: Double, context: Context) -> CAShapeLayer {
        let lineX = context.plotArea.x(nx)
        let line = CAShapeLayer()
        line.name = "touch.line"
        let linePath = UIBezierPath()
        linePath.move(to: CGPoint(x: lineX, y: context.plotArea.rect.minY))
        linePath.addLine(to: CGPoint(x: lineX, y: context.plotArea.rect.maxY))
        line.path = linePath.cgPath
        line.strokeColor = context.style.touchLineColor.cgColor
        line.lineWidth = 1
        return line
    }

    private static func ticks(for axis: ChartAxis, in layout: LineChartLayout) -> [AxisTick]? {
        layout.axisTicks.first { $0.axis == axis }?.ticks
    }

    private static func chartAxis(of axis: Axis) -> ChartAxis? {
        if axis == .primary { return .yPrimary }
        if axis == .secondary { return .ySecondary }
        return nil
    }
}
