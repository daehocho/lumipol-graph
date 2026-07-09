package com.lumipol.graph

/** 멀티지표 차트의 선택/슬롯 규칙(도메인 프리, 정수 id). 앱이 지표↔id를 매핑. */
object SeriesSelection {

    /**
     * 지표 토글 후 선택 상태(순서 보존).
     * @param lineItems 최소1 유지 대상(라인) id. 비라인(예: altitude 배경)은 제외.
     * @param maxCount 최대 동시 선택. 초과 추가 시 가장 오래된 것 제거.
     * 규칙: lineItems의 마지막 하나 해제 시도는 무시(원본 반환).
     */
    fun toggled(current: List<Int>, toggling: Int, lineItems: Set<Int>, maxCount: Int): List<Int> {
        val sel = current.toMutableList()
        val idx = sel.indexOf(toggling)
        if (idx >= 0) {
            if (toggling in lineItems) {
                val remaining = sel.count { it in lineItems && it != toggling }
                if (remaining == 0) return current
            }
            sel.removeAt(idx)
        } else {
            if (sel.size >= maxCount) sel.removeAt(0)
            sel.add(toggling)
        }
        return sel
    }

    /**
     * 슬롯 배정: priority 순서로 (selected ∩ withData)를 필터해 앞 slotCount개 id 반환.
     * 반환 index 0=primary, 1=secondary, 2=overlay …
     */
    fun assignSlots(priority: List<Int>, selected: Set<Int>, withData: Set<Int>, slotCount: Int): List<Int> =
        priority.filter { it in selected && it in withData }.take(slotCount)
}
