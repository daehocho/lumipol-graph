package com.lumipol.graph

/** 멀티지표 차트의 선택/슬롯 규칙(도메인 프리, 정수 id). 앱이 지표↔id를 매핑. */
object SeriesSelection {

    /**
     * 지표 토글 후 선택 상태(순서 보존).
     * @param lineItems 최소1 유지 대상(라인) id. 비라인(예: altitude 배경)은 제외.
     * @param maxCount 최대 동시 선택. 초과 추가 시 가장 오래된 것 제거.
     * 규칙:
     *  - 마지막 하나 남은 항목의 해제 시도는 무시(원본 반환) — 선택이 비면 차트에 그릴 게 없다.
     *    라인 지표가 하나도 없는 기록(고도만 측정된 런)에서도 빈 선택으로 떨어지지 않게 한다.
     *  - lineItems의 마지막 하나 해제 시도도 무시.
     *  - 초과 축출도 같은 보호를 받는다: 라인이 하나뿐이면 그것을 건너뛰고 다음으로 오래된 것을
     *    제거한다. 해제만 막고 축출을 열어두면 추가 한 번으로 라인 0개가 되기 때문이다.
     */
    fun toggled(current: List<Int>, toggling: Int, lineItems: Set<Int>, maxCount: Int): List<Int> {
        val sel = current.toMutableList()
        val idx = sel.indexOf(toggling)
        if (idx >= 0) {
            if (sel.size == 1) return current
            if (toggling in lineItems) {
                val remaining = sel.count { it in lineItems && it != toggling }
                if (remaining == 0) return current
            }
            sel.removeAt(idx)
        } else {
            if (sel.size >= maxCount) sel.removeAt(evictIndex(sel, toggling, lineItems))
            sel.add(toggling)
        }
        return sel
    }

    /**
     * 축출 대상 index — 기본은 가장 오래된 것(앞).
     * 추가되는 항목이 비라인이고 선택에 라인이 하나뿐일 때만 그 라인을 건너뛴다(라인 0개 방지).
     * 새 항목이 라인이면 라인 수가 보전되므로 순서 규약대로 그냥 가장 오래된 것을 뺀다.
     * 건너뛸 수 없으면(=선택이 라인 하나뿐) 0 — 그 자리를 새 항목이 대체한다.
     */
    private fun evictIndex(sel: List<Int>, toggling: Int, lineItems: Set<Int>): Int {
        val protectLine = toggling !in lineItems && sel.count { it in lineItems } == 1
        val idx = sel.indexOfFirst { !(protectLine && it in lineItems) }
        return if (idx >= 0) idx else 0
    }

    /**
     * 선택 상태를 실제 표시 가능한 항목으로 정리 — 데이터 없는 지표를 칩에서 숨기는 앱이,
     * 숨긴 항목이 [maxCount] 자리를 잠식해 보이는 지표를 밀어내는 것을 막는다.
     * 기본 선택값을 그대로 넣고 호출하면 그 기록에 맞는 초기 선택이 된다.
     *
     * @param available 표시 가능한 id — 보통 `PaceSeriesResult.availableSeries`.
     * @param linePriority 라인 지표 id(우선순위 순). 정리 후 라인이 하나도 없으면 여기서 채운다.
     * @return 순서 보존(앞이 가장 오래됨). 결과 크기는 항상 [maxCount] 이하 — 채워 넣은 라인도
     *   이 상한 안에 들어간다. available이 비거나 [maxCount]가 0 이하면 빈 리스트.
     */
    fun normalized(
        current: List<Int>,
        available: Set<Int>,
        linePriority: List<Int>,
        maxCount: Int,
    ): List<Int> {
        if (maxCount <= 0) return emptyList()
        val kept = current.filter { it in available }.takeLast(maxCount)
        if (kept.any { it in linePriority }) return kept
        val fallback = linePriority.firstOrNull { it in available } ?: return kept
        // 채운 라인이 초과분에 밀려 다시 빠지지 않도록 자리를 먼저 비운다(가장 오래된 것부터 제거).
        // 채운 라인은 맨 앞 = 축출 1순위 자리에 놓는다. 사용자가 고른 게 아니라 코어가 보충한 것이라
        // 가장 먼저 밀려나는 게 맞고, "유일한 라인"인 동안은 [toggled]의 축출 보호가 지켜준다.
        return listOf(fallback) + kept.takeLast(maxCount - 1)
    }

    /**
     * 슬롯 배정: priority 순서로 (selected ∩ withData)를 필터해 앞 slotCount개 id 반환.
     * 반환 index 0=primary, 1=secondary, 2=overlay …
     */
    fun assignSlots(priority: List<Int>, selected: Set<Int>, withData: Set<Int>, slotCount: Int): List<Int> =
        priority.filter { it in selected && it in withData }.take(slotCount)
}
