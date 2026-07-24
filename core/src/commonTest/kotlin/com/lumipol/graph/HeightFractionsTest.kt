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

    @Test
    fun min_span_flattens_noise_instead_of_filling_plot() {
        // 고저차 0.25m를 자체 min~max로만 나누면 0~1 전 높이를 채운다(노이즈가 산맥이 됨).
        // minSpan 0.5를 주면 절반까지만 올라간다 — 전처리의 평지 컷을 대체하는 지점.
        assertEquals(listOf(0.0, 1.0), heightFractions(listOf(10.0, 10.25)))
        assertEquals(listOf(0.0, 0.5), heightFractions(listOf(10.0, 10.25), minSpan = 0.5))
    }

    @Test
    fun min_span_does_not_shrink_real_elevation() {
        // 실측 고저차가 하한보다 크면 그대로 자체 min~max 정규화.
        assertEquals(listOf(0.0, 0.5, 1.0), heightFractions(listOf(0.0, 50.0, 100.0), minSpan = 0.5))
    }
}
