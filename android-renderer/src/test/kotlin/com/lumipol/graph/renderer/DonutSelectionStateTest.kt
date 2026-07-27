package com.lumipol.graph.renderer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 홀더는 코어 토글 규칙을 그대로 위임한다 — 앱이 레전드에서 부르는 진입점.
class DonutSelectionStateTest {

    @Test
    fun toggleAppliesCoreTransitions() {
        val state = DonutSelectionState(null, setOf(1, 2, 3))
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
        assertEquals(4, DonutSelectionState(4, setOf(4)).selectedIndex)
    }

    @Test
    fun toggleIgnoresIndexNotInLayout() {
        // iOS testSelectSegmentIgnoresIndexNotInLayout 짝 — 레이아웃에 없는 인덱스(범위 밖,
        // value<=0으로 필터된 세그먼트)를 탭해도 상태가 바뀌면 안 된다. 그렇지 않으면 도넛 전체가
        // "선택 아님" 취급으로 디밍되고, 센터 라벨도 매칭 세그먼트가 없어 사라지며, 자동 해제
        // 타이머가 걸려 스퓨리어스 onSelectSegment(null)까지 발화한다.
        val state = DonutSelectionState(1, setOf(0, 1, 2))
        state.toggle(5) // 레이아웃 밖 인덱스
        assertEquals(1, state.selectedIndex, "레이아웃에 없는 인덱스는 무시(상태 불변)")
    }
}
