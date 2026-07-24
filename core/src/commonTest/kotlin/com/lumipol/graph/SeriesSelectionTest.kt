package com.lumipol.graph

import kotlin.test.Test
import kotlin.test.assertEquals

class SeriesSelectionTest {
    // 앱 매핑 규약: pace=0, heartRate=1, cadence=2, altitude=3.
    // 전체 표시 우선순위(고도 포함) — normalized/assignSlots 폴백에 쓴다.
    private val priority = listOf(0, 1, 2, 3)

    // MARK: toggled

    @Test fun add_metric_appends_in_order() {
        assertEquals(listOf(0, 1), SeriesSelection.toggled(listOf(0), 1))
    }

    @Test fun remove_selected_metric() {
        assertEquals(listOf(0), SeriesSelection.toggled(listOf(0, 3), 3))
    }

    @Test fun keep_last_item() {
        // 마지막 하나 남은 항목의 해제는 무시 — 선택이 비면 그릴 게 없다. 라인 여부 무관.
        assertEquals(listOf(3), SeriesSelection.toggled(listOf(3), 3))
        assertEquals(listOf(0), SeriesSelection.toggled(listOf(0), 0))
    }

    @Test fun no_max_count_allows_four_or_more() {
        // 상한이 없으므로 네 번째 추가도 축출 없이 그대로 붙는다.
        assertEquals(listOf(0, 1, 2, 3), SeriesSelection.toggled(listOf(0, 1, 2), 3))
    }

    @Test fun remove_from_middle_preserves_order() {
        assertEquals(listOf(0, 2, 3), SeriesSelection.toggled(listOf(0, 1, 2, 3), 1))
    }

    // MARK: normalized

    @Test fun normalized_drops_unavailable_preserving_order() {
        assertEquals(listOf(0, 2), SeriesSelection.normalized(listOf(0, 1, 2), setOf(0, 2), priority))
    }

    @Test fun normalized_keeps_altitude_only() {
        // 고도만 측정된 기록 — 라인이 없어도 고도를 그대로 둔다(빈 리스트로 만들지 않는다).
        assertEquals(listOf(3), SeriesSelection.normalized(listOf(0, 1, 3), setOf(3), priority))
    }

    @Test fun normalized_fills_from_priority_when_all_dropped() {
        // 저장 선택이 전부 미가용 → priority의 첫 가용 항목 하나로 채운다(고도 포함).
        assertEquals(listOf(3), SeriesSelection.normalized(listOf(0, 1), setOf(3), priority))
        assertEquals(listOf(2), SeriesSelection.normalized(listOf(0, 1), setOf(2), priority))
    }

    @Test fun normalized_empty_when_nothing_available() {
        assertEquals(emptyList(), SeriesSelection.normalized(listOf(0, 1, 3), emptySet(), priority))
    }

    // MARK: assignSlots

    @Test fun assign_slots_priority_and_data_gated() {
        // priority [0,1,2,3], 선택 {0,1,2}, 데이터 {0,1} → [0,1]
        assertEquals(listOf(0, 1), SeriesSelection.assignSlots(priority, setOf(0, 1, 2), setOf(0, 1)))
    }

    @Test fun assign_slots_no_cap_allows_many_overlays() {
        // 상한이 없으므로 셋 넘게(overlay 슬롯 여러 개) 반환한다.
        assertEquals(listOf(0, 1, 2, 3), SeriesSelection.assignSlots(priority, setOf(0, 1, 2, 3), setOf(0, 1, 2, 3)))
    }

    @Test fun assign_slots_empty_when_no_data() {
        assertEquals(emptyList(), SeriesSelection.assignSlots(priority, setOf(0, 1, 2), emptySet()))
    }
}
