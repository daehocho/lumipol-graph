import Foundation

/// X축 줌 상태 — 보이는 도메인 구간 계산 전담 (UIKit 비의존, 단위 테스트 대상).
/// 핀치·팬 제스처 값을 도메인 구간으로 환산하고 전체 범위·최대 배율로 클램프한다
struct ZoomState: Equatable {
    let fullDomain: ClosedRange<Double>
    private(set) var window: ClosedRange<Double>

    init(fullDomain: ClosedRange<Double>) {
        self.fullDomain = fullDomain
        self.window = fullDomain
    }

    var isZoomed: Bool { window != fullDomain }

    /// 현재 배율 (1 = 전체 보기)
    var scale: Double { fullSpan / span }

    private var span: Double { window.upperBound - window.lowerBound }
    private var fullSpan: Double { fullDomain.upperBound - fullDomain.lowerBound }

    /// 핀치 확대/축소. anchor는 플롯 내 위치(0~1) — 그 지점의 도메인 값이 제자리에 남는다.
    mutating func pinch(by gestureScale: Double, anchor: Double, maxScale: Double) {
        guard gestureScale > 0 else { return }
        let targetSpan = min(max(span / gestureScale, fullSpan / maxScale), fullSpan)
        let anchorValue = window.lowerBound + anchor * span
        place(lower: anchorValue - anchor * targetSpan, span: targetSpan)
    }

    /// 플롯 폭 대비 드래그 비율만큼 좌우 이동 (오른쪽 드래그 = 이전 구간).
    mutating func pan(byFraction fraction: Double) {
        place(lower: window.lowerBound - fraction * span, span: span)
    }

    /// 프로그래매틱 줌·테스트용 — 폭 유지한 채 전체 범위로 클램프.
    mutating func setWindow(_ target: ClosedRange<Double>) {
        let targetSpan = target.upperBound - target.lowerBound
        var span = min(targetSpan, fullSpan)

        // 목표 오버플로우 시 회전 공간 확보용으로 스팬 제한
        if target.lowerBound + span > fullDomain.upperBound {
            span = min(span, fullSpan / 2)
        }

        place(lower: target.lowerBound, span: span)
    }

    mutating func reset() { window = fullDomain }

    private mutating func place(lower: Double, span: Double) {
        let clamped = min(max(lower, fullDomain.lowerBound), fullDomain.upperBound - span)
        window = clamped...(clamped + span)
    }
}
