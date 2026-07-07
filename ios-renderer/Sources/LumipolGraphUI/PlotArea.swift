import UIKit
import LumipolGraph

/// 코어의 값-공간 정규화 좌표(0.0~1.0)를 플롯 영역 픽셀 좌표로 변환한다.
/// 화면 상하 반전(예: 페이스 — 위=빠름)은 렌더러 책임이며, `invertedAxes`에 든 축만 뒤집는다.
struct PlotArea: Equatable {
    let rect: CGRect
    let invertedAxes: Set<Axis>

    init(bounds: CGRect, insets: UIEdgeInsets, invertedAxes: Set<Axis> = []) {
        self.rect = bounds.inset(by: insets)
        self.invertedAxes = invertedAxes
    }

    var isRenderable: Bool { rect.size.width > 0 && rect.size.height > 0 }

    func x(_ nx: Double) -> CGFloat {
        rect.minX + CGFloat(nx) * rect.width
    }

    func y(_ ny: Double, axis: Axis) -> CGFloat {
        let fractionFromTop = invertedAxes.contains(axis) ? ny : 1.0 - ny
        return rect.minY + CGFloat(fractionFromTop) * rect.height
    }

    func point(_ np: NormalizedPoint, axis: Axis) -> CGPoint {
        CGPoint(x: x(np.x), y: y(np.y, axis: axis))
    }

    /// 축 반전 여부와 무관하게 "값이 클수록 위" 규칙만 적용하는 y 변환.
    /// 코어가 이미 자체 정규화한 오버레이 시리즈(축 없음) 전용 — 호스트 축의 `invertedAxes`를 무시한다.
    func yIgnoringInversion(_ ny: Double) -> CGFloat {
        rect.minY + CGFloat(1.0 - ny) * rect.height
    }

    /// 오버레이 시리즈 전용 포인트 변환 — `yIgnoringInversion` 참고.
    func pointIgnoringInversion(_ np: NormalizedPoint) -> CGPoint {
        CGPoint(x: x(np.x), y: yIgnoringInversion(np.y))
    }

    /// 터치 x(픽셀) → 정규화 x. 플롯 영역 밖은 0~1로 클램프.
    func normalizedX(at px: CGFloat) -> Double {
        guard rect.width > 0 else { return 0 }
        return Double(min(max((px - rect.minX) / rect.width, 0), 1))
    }
}
