package com.lumipol.graph

/** 멀티지표 차트의 선택/슬롯 규칙(도메인 프리, 정수 id). 앱이 지표↔id를 매핑. */
object SeriesSelection {

    /**
     * 지표 토글 후 선택 상태(순서 보존, 뒤가 최신).
     * 규칙은 하나뿐: 마지막 하나 남은 항목의 해제 시도는 무시(원본 반환) — 선택이 비면
     * 차트에 그릴 게 없다. 상한·라인 최소1·축출은 없다(동시 선택 무제한, 고도 단독 허용).
     */
    fun toggled(current: List<Int>, toggling: Int): List<Int> {
        val sel = current.toMutableList()
        val idx = sel.indexOf(toggling)
        if (idx >= 0) {
            if (sel.size == 1) return current
            sel.removeAt(idx)
        } else {
            sel.add(toggling)
        }
        return sel
    }

    /**
     * 선택 상태를 실제 표시 가능한 항목으로 정리(순서 보존) — 데이터 없는 지표를 칩에서 숨기는
     * 앱에서 저장된 선택이 현재 기록에 안 맞을 때 쓴다. 전부 걸러지면 [priority]의 첫 가용 항목
     * 하나로 채운다(빈 선택 방지).
     *
     * @param available 표시 가능한 id — 보통 `PaceSeriesResult.availableSeries`.
     * @param priority 폴백 우선순위 — **라인만이 아니라 고도 포함 전체 표시 우선순위**.
     *   정리 후 아무것도 안 남으면 여기서 첫 가용 항목을 채운다. available이 비면 빈 리스트.
     */
    fun normalized(current: List<Int>, available: Set<Int>, priority: List<Int>): List<Int> {
        val kept = current.filter { it in available }
        if (kept.isNotEmpty()) return kept
        val fallback = priority.firstOrNull { it in available } ?: return emptyList()
        return listOf(fallback)
    }

    /**
     * 슬롯 배정: priority 순서로 (selected ∩ withData)를 필터해 반환.
     * 반환 index 0=primary, 1=secondary, 2+=overlay. 상한 없음 — overlay는 몇 개든 나올 수 있다.
     */
    fun assignSlots(priority: List<Int>, selected: Set<Int>, withData: Set<Int>): List<Int> =
        priority.filter { it in selected && it in withData }
}
