package com.lumipol.graph.renderer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 홀더는 코어 토글 규칙을 그대로 위임한다 — 앱이 레전드에서 부르는 진입점.
class DonutSelectionStateTest {

    @Test
    fun toggleAppliesCoreTransitions() {
        val state = DonutSelectionState(null)
        state.toggle(2)
        assertEquals(2, state.selectedIndex)   // 선택
        state.toggle(2)
        assertNull(state.selectedIndex)        // 같은 인덱스 재요청 → 해제
        state.toggle(1)
        state.toggle(3)
        assertEquals(3, state.selectedIndex)   // 다른 인덱스 → 이동
        state.toggle(null)
        assertNull(state.selectedIndex)        // null → 해제
    }

    @Test
    fun initialValueIsCarried() {
        assertEquals(4, DonutSelectionState(4).selectedIndex)
    }
}
