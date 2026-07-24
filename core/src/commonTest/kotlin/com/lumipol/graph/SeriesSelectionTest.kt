package com.lumipol.graph

import kotlin.test.Test
import kotlin.test.assertEquals

class SeriesSelectionTest {
    // 앱 매핑 규약과 동일: pace=0, heartRate=1, cadence=2, altitude=3. 라인=,{0,1,2}, max=3.
    private val lines = setOf(0, 1, 2)

    @Test fun add_metric() {
        assertEquals(listOf(0, 1), SeriesSelection.toggled(listOf(0), 1, lines, 3))
    }

    @Test fun remove_selected_non_line() {
        assertEquals(listOf(0), SeriesSelection.toggled(listOf(0, 3), 3, lines, 3))
    }

    @Test fun keep_last_item_even_when_not_a_line() {
        // 고도만 측정된 기록: 라인 최소1 규칙이 안 걸리는 항목이라도 마지막 하나는 못 끈다.
        // (끄면 선택이 비어 "데이터 없음"이 뜨는데, 정작 고도 데이터는 있는 상태가 된다.)
        assertEquals(listOf(3), SeriesSelection.toggled(listOf(3), 3, lines, 3))
    }

    @Test fun evict_oldest_when_exceeding_max() {
        // [pace,heartRate,altitude] + cadence → 가장 오래된 pace 제거
        assertEquals(listOf(1, 3, 2), SeriesSelection.toggled(listOf(0, 1, 3), 2, lines, 3))
    }

    @Test fun evict_skips_the_only_line_when_adding_a_non_line() {
        // 비라인 id가 둘 이상인 앱(4=배경 페이스존 등) 기준. 축출이 무조건 index 0이면
        // 유일한 라인 pace가 밀려 라인 0개가 된다 — 해제만 막고 축출을 열어두면 뚫리는 구멍.
        assertEquals(listOf(0, 4, 5), SeriesSelection.toggled(listOf(0, 3, 4), 5, lines, 3))
        // 새 항목이 라인이면 라인 수가 보전되므로 순서 규약대로 가장 오래된 것을 뺀다.
        assertEquals(listOf(3, 4, 1), SeriesSelection.toggled(listOf(0, 3, 4), 1, lines, 3))
    }

    @Test fun keep_at_least_one_line() {
        // 유일한 라인(pace) 해제 시도 무시(원본 반환)
        assertEquals(listOf(0, 3), SeriesSelection.toggled(listOf(0, 3), 0, lines, 3))
    }

    @Test fun remove_line_when_another_line_remains() {
        assertEquals(listOf(1), SeriesSelection.toggled(listOf(0, 1), 0, lines, 3))
    }

    @Test fun assign_slots_priority_and_data_gated() {
        // priority [0,1,2], 선택 {0,1,2}, 데이터 {0,1}(cadence 데이터 없음) → [0,1]
        assertEquals(listOf(0, 1), SeriesSelection.assignSlots(listOf(0, 1, 2), setOf(0, 1, 2), setOf(0, 1), 3))
    }

    @Test fun assign_slots_takes_first_n() {
        assertEquals(listOf(0, 1, 2), SeriesSelection.assignSlots(listOf(0, 1, 2), setOf(0, 1, 2), setOf(0, 1, 2), 3))
    }

    @Test fun assign_slots_empty_when_no_data() {
        assertEquals(emptyList(), SeriesSelection.assignSlots(listOf(0, 1, 2), setOf(0, 1, 2), emptySet(), 3))
    }

    // MARK: normalized — 앱이 각자 들고 있던 선택 정리 로직의 코어 단일 소스

    @Test fun normalized_drops_unavailable() {
        // 기본 선택 [pace,heartRate,altitude] + 심박·고도 미측정 → pace만
        assertEquals(listOf(0), SeriesSelection.normalized(listOf(0, 1, 3), setOf(0, 2), PaceSeriesId.LINE_PRIORITY, 3))
    }

    @Test fun normalized_fills_line_when_all_lines_dropped() {
        // 페이스·심박이 없고 케이던스만 있는 기록 → 우선순위상 첫 가용 라인을 채운다
        assertEquals(
            listOf(2, 3),
            SeriesSelection.normalized(listOf(0, 1, 3), setOf(2, 3), PaceSeriesId.LINE_PRIORITY, 3))
    }

    @Test fun normalized_keeps_non_line_only_when_no_line_available() {
        // 고도만 측정된 기록 — 채울 라인이 없으면 그대로 둔다(빈 리스트로 만들지 않는다)
        assertEquals(listOf(3), SeriesSelection.normalized(listOf(0, 1, 3), setOf(3), PaceSeriesId.LINE_PRIORITY, 3))
    }

    @Test fun normalized_respects_max_count_when_filling() {
        // 채운 라인이 초과분에 밀려 다시 빠지면 안 된다(가장 오래된 것부터 제거).
        val r = SeriesSelection.normalized(listOf(3, 4, 5), setOf(2, 3, 4, 5), PaceSeriesId.LINE_PRIORITY, 3)
        assertEquals(listOf(2, 4, 5), r)
    }

    @Test fun normalized_empty_when_nothing_available() {
        assertEquals(emptyList(), SeriesSelection.normalized(listOf(0, 1, 3), emptySet(), PaceSeriesId.LINE_PRIORITY, 3))
    }

    @Test fun normalized_never_exceeds_max_count() {
        // maxCount=1: 채운 라인 하나만. 0 이하면 채우기까지 막아 상한을 넘기지 않는다.
        assertEquals(listOf(0), SeriesSelection.normalized(listOf(1, 3), setOf(0, 3), PaceSeriesId.LINE_PRIORITY, 1))
        assertEquals(emptyList(), SeriesSelection.normalized(listOf(0, 1, 3), setOf(0, 1, 3), PaceSeriesId.LINE_PRIORITY, 0))
    }
}
