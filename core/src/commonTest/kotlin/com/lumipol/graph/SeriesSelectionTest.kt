package com.lumipol.graph

import com.lumipol.graph.model.Axis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SeriesSelectionTest {
    // 앱 매핑 규약: pace=0, heartRate=1, cadence=2, altitude=3.
    // 전체 표시 우선순위(고도 포함) — normalized/assignSlots 폴백에 쓴다.
    private val priority = listOf(0, 1, 2, 3)

    // MARK: PaceSeriesId 우선순위 상수

    @Test fun display_priority_includes_altitude_after_lines() {
        // normalized의 priority 계약(고도 포함 전체 표시 우선순위)을 코어 상수가 직접 충족해야 한다.
        assertEquals(PaceSeriesId.LINE_PRIORITY + PaceSeriesId.ALTITUDE, PaceSeriesId.DISPLAY_PRIORITY)
    }

    @Test fun normalized_with_display_priority_keeps_altitude_only_record() {
        // 고도만 측정된 기록 + 라인만 저장된 선택 — DISPLAY_PRIORITY면 고도로 폴백돼 빈 선택이 안 된다.
        // (LINE_PRIORITY를 넘기면 emptyList — 0.20.0 이전 "데이터 없음" 버그 재도입 경로.)
        assertEquals(
            listOf(PaceSeriesId.ALTITUDE),
            SeriesSelection.normalized(
                current = listOf(PaceSeriesId.PACE, PaceSeriesId.HEART),
                available = setOf(PaceSeriesId.ALTITUDE),
                priority = PaceSeriesId.DISPLAY_PRIORITY,
            ),
        )
    }

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

    // MARK: slotAxis

    @Test fun slot_axis_first_primary_rest_all_secondary() {
        // 0.29.0 규약: 오버레이 슬롯 폐지 — 1 이후는 전부 보조축(도메인 공유).
        assertEquals(Axis.PRIMARY, SeriesSelection.slotAxis(0))
        assertEquals(Axis.SECONDARY, SeriesSelection.slotAxis(1))
        assertEquals(Axis.SECONDARY, SeriesSelection.slotAxis(2))
        assertEquals(Axis.SECONDARY, SeriesSelection.slotAxis(3))
    }

    @Test fun slot_axis_rejects_negative_index() {
        assertFailsWith<IllegalArgumentException> { SeriesSelection.slotAxis(-1) }
    }

    // MARK: slotAxes (0.40.0 — 스케일 그룹 공유)

    @Test fun slot_axes_merges_shared_scale_group_onto_primary() {
        val shared = PaceSeriesId.SHARED_SCALE_IDS
        // 심박+케이던스 → 둘 다 PRIMARY(왼쪽 한 축) — 좌우 중복 눈금 제거
        assertEquals(
            listOf(Axis.PRIMARY, Axis.PRIMARY),
            SeriesSelection.slotAxes(listOf(PaceSeriesId.HEART, PaceSeriesId.CADENCE), shared),
        )
        // 페이스+심박+케이던스 → 기존과 동일(페이스 PRIMARY, 심박∪케이던스 SECONDARY)
        assertEquals(
            listOf(Axis.PRIMARY, Axis.SECONDARY, Axis.SECONDARY),
            SeriesSelection.slotAxes(listOf(PaceSeriesId.PACE, PaceSeriesId.HEART, PaceSeriesId.CADENCE), shared),
        )
        // 페이스+심박 → 기존과 동일
        assertEquals(
            listOf(Axis.PRIMARY, Axis.SECONDARY),
            SeriesSelection.slotAxes(listOf(PaceSeriesId.PACE, PaceSeriesId.HEART), shared),
        )
        // 단일·빈 슬롯
        assertEquals(listOf(Axis.PRIMARY), SeriesSelection.slotAxes(listOf(PaceSeriesId.CADENCE), shared))
        assertEquals(emptyList(), SeriesSelection.slotAxes(emptyList(), shared))
    }

    @Test fun inverted_axes_for_uses_assigned_axes() {
        val axes = listOf(Axis.PRIMARY, Axis.SECONDARY, Axis.SECONDARY)
        assertEquals(emptySet(), SeriesSelection.invertedAxesFor(-1, axes))
        assertEquals(setOf(Axis.PRIMARY), SeriesSelection.invertedAxesFor(0, axes))
        assertEquals(setOf(Axis.SECONDARY), SeriesSelection.invertedAxesFor(1, axes))
        assertFailsWith<IllegalArgumentException> { SeriesSelection.invertedAxesFor(3, axes) }
    }
}
