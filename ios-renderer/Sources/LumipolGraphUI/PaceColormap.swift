import UIKit
import LumipolGraph

/// 막대 하나의 색을 결정할 때 넘기는 입력 — `ChartStyle.barColorProvider` 오버라이드 전용.
/// 기본 색 공식은 코어 `PaceColormap`(B6)이 단독 소유한다 — 렌더러 복제본(구 defaultPaceColor)은 삭제됨.
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

extension UIColor {
    /// 코어 색 값(0xAARRGGBB) → UIColor. 코어가 색을, 렌더러가 플랫폼 타입 변환만(경계 정책 §1-4).
    convenience init(argb: Int64) {
        let a = CGFloat((argb >> 24) & 0xFF) / 255
        let r = CGFloat((argb >> 16) & 0xFF) / 255
        let g = CGFloat((argb >> 8) & 0xFF) / 255
        let b = CGFloat(argb & 0xFF) / 255
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}
