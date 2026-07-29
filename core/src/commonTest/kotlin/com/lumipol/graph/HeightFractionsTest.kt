package com.lumipol.graph

import com.lumipol.graph.query.heightFractions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun nan_value_does_not_poison_other_fractions() {
        // 0.49.0: min/max 스캔은 NaN을 무시한다(고도 눈금 overlayAxisTicks와 같은 규칙) —
        // 종전 minOrNull()은 NaN을 전파해 실루엣 전체가 사라졌고, 눈금만 정상 min/max를 내
        // "라벨-실루엣 정렬" 불변식이 깨졌다. 오염된 점만 NaN fraction이 된다.
        val f = heightFractions(listOf(10.0, Double.NaN, 30.0))
        assertEquals(0.0, f[0])
        assertTrue(f[1].isNaN())
        assertEquals(1.0, f[2])
    }

    @Test
    fun all_nan_maps_to_zero_like_flat() {
        assertEquals(listOf(0.0, 0.0), heightFractions(listOf(Double.NaN, Double.NaN)))
    }
}
