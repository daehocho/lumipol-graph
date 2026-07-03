import LumipolGraph

/// 축 tick의 (value, position) 선형 관계로 원본 값 ↔ 정규화 위치(0~1)를 상호 변환한다.
/// X축은 데이터 맞춤 도메인이라 끝 tick이 position 1 안쪽일 수 있지만,
/// tick(value, position)은 항상 같은 선형 관계 위에 있으므로 양끝 tick으로 기울기를 얻는 방식은 유효하다.
struct AxisScale {
    private let baseValue: Double
    private let basePosition: Double
    private let valuePerPosition: Double

    /// tick이 2개 미만이거나 위치·값 간격이 0이면 변환 불능 → nil.
    init?(ticks: [AxisTick]) {
        guard let first = ticks.first, let last = ticks.last,
              last.position != first.position, last.value != first.value
        else { return nil }
        baseValue = first.value
        basePosition = first.position
        valuePerPosition = (last.value - first.value) / (last.position - first.position)
    }

    func value(atPosition position: Double) -> Double {
        baseValue + (position - basePosition) * valuePerPosition
    }

    func position(ofValue value: Double) -> Double {
        basePosition + (value - baseValue) / valuePerPosition
    }
}
