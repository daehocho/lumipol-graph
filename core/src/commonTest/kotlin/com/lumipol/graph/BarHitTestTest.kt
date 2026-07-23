package com.lumipol.graph

import com.lumipol.graph.query.barIndexAtX
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BarHitTestTest {
    // plotMinX=44, plotWidth=200, count=4 → slot=50. 슬롯 중앙이 각 인덱스로.
    @Test fun slotCenters() {
        assertEquals(0, barIndexAtX(44.0 + 25, 44.0, 200.0, 4))
        assertEquals(1, barIndexAtX(44.0 + 75, 44.0, 200.0, 4))
        assertEquals(3, barIndexAtX(44.0 + 175, 44.0, 200.0, 4))
    }

    // 슬롯 경계는 floor 규칙(다음 슬롯 시작).
    @Test fun slotBoundaryFloors() {
        assertEquals(1, barIndexAtX(44.0 + 50, 44.0, 200.0, 4)) // 정확히 slot 경계 → index 1
    }

    // 플롯 좌/우 밖은 0..count-1로 클램프.
    @Test fun clampsOutOfBounds() {
        assertEquals(0, barIndexAtX(0.0, 44.0, 200.0, 4))       // 왼쪽 밖
        assertEquals(3, barIndexAtX(1000.0, 44.0, 200.0, 4))    // 오른쪽 밖
    }

    // 축퇴(count/width ≤ 0)는 null.
    @Test fun degenerateReturnsNull() {
        assertNull(barIndexAtX(50.0, 44.0, 200.0, 0))
        assertNull(barIndexAtX(50.0, 44.0, 0.0, 4))
    }
}
