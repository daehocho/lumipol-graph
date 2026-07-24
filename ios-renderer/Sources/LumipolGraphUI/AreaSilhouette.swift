import UIKit
import LumipolGraph

/// 배경 고도 실루엣용 도메인 포인트(원본 값 x, y).
public struct AreaPoint {
    public let x: Double
    public let y: Double
    public init(x: Double, y: Double) {
        self.x = x
        self.y = y
    }
}

/// 페이스 라인 뒤에 깔리는 고도 실루엣(장식). 축·스크럽과 무관한 순수 프레젠테이션 레이어.
enum AreaSilhouette {
    /// 값들을 자체 min~max로 0~1 정규화 — 코어 질의(`heightFractions`)에 위임(interpolatedY와
    /// 같은 이관 사유: 플랫폼 중립 수학). 축퇴(전부 동일) 시 모두 0(평지) 의미론 유지.
    /// minSpan은 노이즈가 산맥으로 보이지 않게 하는 분모 하한(`ChartStyle.areaMinValueSpan`).
    static func heightFractions(_ values: [Double], minSpan: Double = 0) -> [Double] {
        HeightFractionsKt.heightFractions(
            values: values.map { KotlinDouble(value: $0) }, minSpan: minSpan
        ).map(\.doubleValue)
    }

    /// 도메인 area 포인트 → 실루엣 CAShapeLayer. 2점 미만이거나 렌더 불가 플롯이면 nil.
    /// x: xScale로 정규화 위치 산출 후 plotArea.x. y: heightFraction × areaHeightFraction, 바닥(maxY) 기준 위로.
    /// 축 반전 무관(자체 매핑) — plotArea.y는 쓰지 않는다.
    static func layer(
        points: [AreaPoint], xScale: AxisScale, plotArea: PlotArea, style: ChartStyle
    ) -> CAShapeLayer? {
        guard points.count >= 2, plotArea.isRenderable else { return nil }
        let fractions = heightFractions(points.map { $0.y }, minSpan: style.areaMinValueSpan)
        let baseY = plotArea.rect.maxY
        let usableHeight = style.areaHeightFraction * plotArea.rect.height
        func pixel(_ index: Int) -> CGPoint {
            // 시리즈 x-도메인보다 넓은 area 데이터는 정규화 위치가 0~1을 벗어난다 —
            // 1x에서는 클립 마스크가 없으므로 좌표를 플롯 영역으로 클램프해 번짐을 막는다.
            let nx = min(max(xScale.position(ofValue: points[index].x), 0), 1)
            let px = plotArea.x(nx)
            let py = baseY - CGFloat(fractions[index]) * usableHeight
            return CGPoint(x: px, y: py)
        }
        let path = UIBezierPath()
        path.move(to: pixel(0))
        for index in 1..<points.count { path.addLine(to: pixel(index)) }
        path.addLine(to: CGPoint(x: pixel(points.count - 1).x, y: baseY))
        path.addLine(to: CGPoint(x: pixel(0).x, y: baseY))
        path.close()
        let layer = CAShapeLayer()
        layer.name = "area.altitude"
        layer.path = path.cgPath
        layer.fillColor = style.areaFillColor.cgColor
        layer.strokeColor = nil
        return layer
    }
}
