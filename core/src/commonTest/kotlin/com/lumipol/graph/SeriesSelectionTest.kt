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

    @Test fun evict_oldest_when_exceeding_max() {
        // [pace,heartRate,altitude] + cadence → 가장 오래된 pace 제거
        assertEquals(listOf(1, 3, 2), SeriesSelection.toggled(listOf(0, 1, 3), 2, lines, 3))
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
}
