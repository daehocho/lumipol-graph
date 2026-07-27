import UIKit
import LumipolGraph

/// 터치 지점의 근접점 마커 레이어(수직선 + 시리즈별 점)를 만들고, 시리즈별 포맷 값을 반환한다.
/// 창 필터·스냅 소스 선택·정규화 좌표는 코어 `LineChartEngine.nearestScrub`(B2)가 확정한다 —
/// 양 플랫폼 동일 로직. 렌더러는 플랫폼 좌표 변환과 레이어 조립만 한다.
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
        /// 수직선 기준으로 스냅된 원본 도메인 x(첫 main 근접점). 배경 area 보간에 사용.
        let snappedX: Double
    }

    /// 시리즈 없는 차트(배경 area 단독)용 마커 — 스냅 격자(시리즈 포인트)가 없으므로
    /// rawX를 그대로 수직선 위치로 쓴다(연속 스크럽). 시리즈 값이 없어 valuesBySeriesId는 빈 딕셔너리.
    static func makeBackgroundOnly(atRawX rawX: Double, context: Context) -> Result? {
        let xDomain = context.layout.domains.x
        guard context.plotArea.isRenderable, xDomain.max > xDomain.min else { return nil }
        let epsilon = ScrubKt.SCRUB_WINDOW_EPSILON
        let rawNx = xDomain.normalize(v: rawX)
        guard rawNx >= -epsilon, rawNx <= 1 + epsilon else { return nil }
        let nx = min(max(rawNx, 0), 1)
        let container = CALayer()
        container.name = "touch.marker"
        container.addSublayer(verticalLine(atNx: nx, context: context))
        return Result(
            layer: container, valuesBySeriesId: [:],
            snappedX: xDomain.denormalize(t: nx)
        )
    }

    /// 원본 도메인 x 기준 마커. 표시 불가(플롯 없음·축 변환 불능·근접점 없음/전부 창밖)면 nil.
    /// 스냅 규칙(main 우선·창 epsilon·오버레이 자체 정규화 y)은 코어 `nearestScrub` 소관.
    static func make(atRawX rawX: Double, context: Context) -> Result? {
        guard context.plotArea.isRenderable,
              let scrub = LineChartEngine.shared.nearestScrub(
                  data: context.data, layout: context.layout, x: rawX
              )
        else { return nil }

        let container = CALayer()
        container.name = "touch.marker"
        container.addSublayer(verticalLine(atNx: scrub.snappedNx, context: context))

        var valuesBySeriesId: [String: String] = [:]
        for p in scrub.perSeries {
            valuesBySeriesId[p.seriesId] = context.formatter(p.chartAxis, p.y)
            guard let ny = p.ny?.doubleValue else { continue } // 도트 불능(오버레이가 layout에 없음) — 값만 전달
            // 오버레이는 라인과 같은 반전 무시 매핑, 축 시리즈는 해당 축 매핑(코어 ny와 좌표계 일치).
            let point = p.role == .overlay
                ? context.plotArea.pointIgnoringInversion(NormalizedPoint(x: p.nx, y: ny))
                : context.plotArea.point(NormalizedPoint(x: p.nx, y: ny), axis: p.axis)
            let dot = CAShapeLayer()
            dot.name = "touch.dot.\(p.seriesId)"
            dot.path = UIBezierPath(
                arcCenter: point, radius: context.style.touchDotRadius,
                startAngle: 0, endAngle: .pi * 2, clockwise: true
            ).cgPath
            // 라인·그라데이션과 같은 seriesColor 리졸버(맵 우선, 축/역할 폴백) — 도트만 다른 색이 되지 않게.
            dot.fillColor = ChartLayerBuilder.seriesColor(
                id: p.seriesId, role: p.role, axis: p.axis, style: context.style
            ).cgColor
            container.addSublayer(dot)
        }
        return Result(layer: container, valuesBySeriesId: valuesBySeriesId, snappedX: scrub.snappedX)
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
}
