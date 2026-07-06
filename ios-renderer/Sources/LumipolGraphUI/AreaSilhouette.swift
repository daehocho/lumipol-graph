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
    /// 값들을 자체 min~max로 0~1 정규화. 빈 배열→빈 배열, 단일/전부 동일→모두 0(평지, 0으로 나눔 방지).
    static func heightFractions(_ values: [Double]) -> [Double] {
        guard let lo = values.min(), let hi = values.max() else { return [] }
        let span = hi - lo
        guard span > 0 else { return values.map { _ in 0 } }
        return values.map { ($0 - lo) / span }
    }
}
