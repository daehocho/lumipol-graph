package com.lumipol.graph

import com.lumipol.graph.query.heightFractions
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 배경 실루엣 높이 정규화 규약 — iOS/Android 렌더러가 각자 들고 있던 min~max 정규화를 코어로
 * 이관(interpolatedY와 같은 이유). 축퇴(전부 동일) 시 **모두 0(평지)** — AxisDomain.normalize의
 * 0.5와 다른, 실루엣 전용의 의도된 의미론이다(양 플랫폼 렌더러 주석과 일치).
 */
class HeightFractionsTest {

    @Test
    fun normalizes_by_own_min_max() {
        assertEquals(listOf(0.0, 0.5, 1.0), heightFractions(listOf(10.0, 20.0, 30.0)))
    }

    @Test
    fun flat_values_map_to_zero_not_half() {
        assertEquals(listOf(0.0, 0.0, 0.0), heightFractions(listOf(7.0, 7.0, 7.0)))
    }

    @Test
    fun empty_input_returns_empty() {
        assertEquals(emptyList(), heightFractions(emptyList()))
    }
}
