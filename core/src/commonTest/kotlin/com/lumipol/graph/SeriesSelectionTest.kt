package com.lumipol.graph

import com.lumipol.graph.model.Axis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

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

    @Suppress("DEPRECATION") // 구 API 동작 보존 검증 — 신 규칙은 slot_axes_* 테스트
    @Test fun slot_axis_first_primary_rest_all_secondary() {
        // 0.29.0 규약: 오버레이 슬롯 폐지 — 1 이후는 전부 보조축(도메인 공유).
        assertEquals(Axis.PRIMARY, SeriesSelection.slotAxis(0))
        assertEquals(Axis.SECONDARY, SeriesSelection.slotAxis(1))
        assertEquals(Axis.SECONDARY, SeriesSelection.slotAxis(2))
        assertEquals(Axis.SECONDARY, SeriesSelection.slotAxis(3))
    }

    @Suppress("DEPRECATION")
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

    // MARK: selectAll / isAllSelected (0.50.0 — 종합 칩)

    @Test fun select_all_appends_missing_in_priority_order() {
        val next = SeriesSelection.selectAll(
            current = listOf(1), available = setOf(0, 1, 2, 3), priority = priority)
        assertEquals(listOf(1, 0, 2, 3), next)  // 현재 순서 보존 + 빠진 것만 priority 순으로 뒤에
    }

    @Test fun select_all_limits_to_available() {
        val next = SeriesSelection.selectAll(
            current = listOf(0), available = setOf(0, 3), priority = priority)
        assertEquals(listOf(0, 3), next)  // 미가용(1, 2)은 추가하지 않는다
    }

    @Test fun select_all_returns_same_instance_when_already_all() {
        val current = listOf(2, 0, 1, 3)
        assertSame(
            current,
            SeriesSelection.selectAll(
                current = current, available = setOf(0, 1, 2, 3), priority = priority),
        )
    }

    @Test fun select_all_keeps_out_of_available_ids() {
        // 저장된 의도(current)에 이 기록에서 미가용인 id(1)가 있어도 버리지 않는다 —
        // 정리는 normalized 소관(toggled와 같은 분업).
        val next = SeriesSelection.selectAll(
            current = listOf(1, 0), available = setOf(0, 3), priority = priority)
        assertEquals(listOf(1, 0, 3), next)
    }

    @Test fun select_all_with_empty_available_returns_current() {
        val current = listOf(0)
        assertSame(
            current,
            SeriesSelection.selectAll(current = current, available = emptySet(), priority = priority),
        )
    }

    @Test fun select_all_covers_available_ids_missing_from_priority() {
        // 앱이 라인 우선순위(고도 없음)를 넘겨도 결과는 available 전부를 포함해야 한다 —
        // 아니면 isAllSelected와 갈려 종합 칩이 영구 비활성 + 재탭 무반응(죽은 칩)이 된다.
        val next = SeriesSelection.selectAll(
            current = listOf(0), available = setOf(0, 1, 2, 3), priority = PaceSeriesId.LINE_PRIORITY)
        assertEquals(listOf(0, 1, 2, 3), next)
        assertTrue(SeriesSelection.isAllSelected(next, setOf(0, 1, 2, 3)))
    }

    @Test fun select_all_appends_unknown_available_ids_in_id_order() {
        // priority에 없는 미래 id(9, 7)도 결정적 순서(id 오름차순)로 붙는다.
        val next = SeriesSelection.selectAll(
            current = listOf(1), available = setOf(1, 9, 7), priority = priority)
        assertEquals(listOf(1, 7, 9), next)
    }

    @Test fun is_all_selected_requires_every_available_id() {
        assertTrue(SeriesSelection.isAllSelected(listOf(3, 1, 0, 2), setOf(0, 1, 2, 3)))
        assertFalse(SeriesSelection.isAllSelected(listOf(0, 1), setOf(0, 1, 2)))
    }

    @Test fun is_all_selected_ignores_out_of_available_ids() {
        assertTrue(SeriesSelection.isAllSelected(listOf(1, 0, 3), setOf(0, 3)))
    }

    @Test fun is_all_selected_false_when_nothing_available() {
        assertFalse(SeriesSelection.isAllSelected(listOf(0), emptySet()))
    }
}
