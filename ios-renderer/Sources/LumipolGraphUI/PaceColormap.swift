import UIKit
import LumipolGraph

/// 막대 하나의 색을 결정할 때 넘기는 입력.
public struct BarPaceColorInput {
    public let value: Double        // 이 막대의 실제 페이스 (sec/unit, 낮을수록 빠름)
    public let fastest: Double      // 이 런 막대들 중 최소값(가장 빠름)
    public let slowest: Double      // 이 런 막대들 중 최대값(가장 느림)
    public let average: Double      // 막대 value 평균(등거리 스플릿 → 런 평균 페이스와 일치)
    public let isPartial: Bool
    public let index: Int
    public let colorRole: BarColorRole
    public init(value: Double, fastest: Double, slowest: Double, average: Double,
                isPartial: Bool, index: Int, colorRole: BarColorRole) {
        self.value = value; self.fastest = fastest; self.slowest = slowest
        self.average = average; self.isPartial = isPartial; self.index = index
        self.colorRole = colorRole
    }
}

public extension ChartStyle {
    /// 기본 연속 팔레트 — 지도 RDRouteColorizer 비색약 3구간 공식 이식.
    /// 빠름(값 낮음) 파랑↔청록, 평균 근처 초록↔노랑, 느림(값 높음) 노랑↔빨강.
    /// 평균(average) 앵커로 pace1/pace2를 잡아 평균 근처에 색 해상도를 더 준다.
    /// fastest==slowest 등 구간 길이 0인 축퇴 케이스는 중간 초록으로 폴백.
    static func defaultPaceColor(_ input: BarPaceColorInput) -> UIColor {
        let f = input.fastest, s = input.slowest, a = input.average, p = input.value
        guard s > f else { return UIColor(red: 0, green: 1, blue: 0, alpha: 1) }
        let pace1 = a - (a - f) * 0.70
        let pace2 = a + (s - a) * 0.25
        let length1 = pace1 - f, length2 = pace2 - pace1, length3 = s - pace2
        func clamp(_ x: Double) -> CGFloat { CGFloat(min(max(x, 0), 1)) }
        if p < pace1 {                                  // 파랑↔청록
            let cv = length1 > 0 ? clamp((pace1 - max(f, p)) / length1) : 0
            return UIColor(red: 0, green: 1 - 0.4 * cv, blue: 1, alpha: 1)
        } else if p < pace2 {                           // 초록↔노랑
            let cv = length2 > 0 ? clamp((pace2 - p) / length2) : 0
            return UIColor(red: 1 - cv, green: 1, blue: 0, alpha: 1)
        } else {                                        // 노랑↔빨강
            let cv = length3 > 0 ? clamp((s - min(s, p)) / length3) : 0
            return UIColor(red: 1, green: cv, blue: 0, alpha: 1)
        }
    }
}
