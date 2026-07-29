package com.lumipol.graph

import com.lumipol.graph.model.Axis

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
     * 반환 index 0=primary, 1 이후=전부 secondary — 축 매핑은 [slotAxis]. 상한 없음.
     */
    fun assignSlots(priority: List<Int>, selected: Set<Int>, withData: Set<Int>): List<Int> =
        priority.filter { it in selected && it in withData }

    /**
     * [assignSlots] 결과 전체 → 슬롯별 축(0.40.0). 슬롯 0 = PRIMARY, 이후 = SECONDARY(도메인 공유) —
     * 단 슬롯 0과 해당 슬롯이 모두 [sharedScaleIds]에 속하면 PRIMARY를 공유한다
     * (심박+케이던스만 선택 시 좌우 중복 눈금 제거 — 도메인은 yValues 합집합으로 병합).
     * role은 전부 MAIN — 축 슬롯에서 OVERLAY는 나오지 않는다(0.29.0).
     * 스케일이 다른 지표를 축 밖에 겹치려면 앱이 직접 OVERLAY를 조립한다.
     *
     * @param sharedScaleIds 한 축을 공유해도 되는 지표 id 그룹 — 보통 [PaceSeriesId.SHARED_SCALE_IDS].
     */
    fun slotAxes(slots: List<Int>, sharedScaleIds: Set<Int>): List<Axis> {
        val first = slots.firstOrNull() ?: return emptyList()
        return slots.mapIndexed { index, id ->
            if (index == 0 || (first in sharedScaleIds && id in sharedScaleIds)) Axis.PRIMARY
            else Axis.SECONDARY
        }
    }

    /**
     * [assignSlots] 결과 index → 축. 0 = PRIMARY, 1 이후 = 전부 SECONDARY(도메인 공유).
     */
    @Deprecated(
        "슬롯 index만으로는 스케일 그룹 병합(심박+케이던스 한 축)을 표현할 수 없다 — slotAxes로 대체(0.40.0)",
        ReplaceWith("slotAxes(slots, sharedScaleIds)"),
    )
    fun slotAxis(index: Int): Axis {
        require(index >= 0) { "index must be >= 0" }
        return if (index == 0) Axis.PRIMARY else Axis.SECONDARY
    }

    /**
     * 화면에서 뒤집을 Y축 집합(0.40.0) — [slotAxes] 결과 기반. 페이스는 "위=빠름"이라
     * 페이스가 앉은 축만 반전한다. [paceSlot]은 [assignSlots] 결과에서 페이스의 index(없으면 음수).
     * 앱 2벌 분기(`paceOnPrimary ? [.primary] : []` / 슬롯 0/1 분기 — AOS는 슬롯 2+에서 반전
     * 누락 버그)를 단일 규칙으로 회수: 페이스가 어느 슬롯이든 그 슬롯의 축을 반전.
     */
    fun invertedAxesFor(paceSlot: Int, axes: List<Axis>): Set<Axis> {
        if (paceSlot < 0) return emptySet()
        require(paceSlot < axes.size) { "paceSlot must be < axes.size" }
        return setOf(axes[paceSlot])
    }

    /**
     * 화면에서 뒤집을 Y축 집합(C5) — [slotAxis] 기반 구 매핑.
     */
    @Deprecated(
        "slotAxis 기반 매핑 — slotAxes 결과를 받는 오버로드로 대체(0.40.0)",
        ReplaceWith("invertedAxesFor(paceSlot, axes)"),
    )
    fun invertedAxesFor(paceSlot: Int): Set<Axis> =
        @Suppress("DEPRECATION")
        if (paceSlot < 0) emptySet() else setOf(slotAxis(paceSlot))
}
