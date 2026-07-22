package com.lumipol.graph

import com.lumipol.graph.query.labelStride
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 균등 라벨 솎아내기 stride 규약 — 막대 차트가 슬롯 폭에 안 맞는 라벨(장거리 42km=43스플릿)을
 * 전부 그려 가로로 겹치던 문제(iOS/Android 공통)를 코어 정수 계산으로 해소한다.
 * i % stride == 0 인 인덱스만 그리면 이웃 표시 라벨 중심 간격 = stride*slot ≥ (labelWidth+gap).
 */
class LabelThinningTest {

    @Test
    fun shows_all_when_labels_fit_within_slot() {
        // slot = 300/10 = 30, 라벨 20 + gap 4 = 24 ≤ 30 → 전부 표시
        assertEquals(1, labelStride(count = 10, plotWidthPx = 300.0, labelWidthPx = 20.0, gapPx = 4.0))
    }

    @Test
    fun thins_when_label_wider_than_slot() {
        // 43스플릿, plot 400 → slot ≈ 9.30. 라벨 40 + gap 4 = 44 → ceil(44/9.30)=5
        assertEquals(5, labelStride(count = 43, plotWidthPx = 400.0, labelWidthPx = 40.0, gapPx = 4.0))
    }

    @Test
    fun stride_exactly_at_slot_boundary_is_one() {
        // 라벨+gap == slot → ceil(1.0)=1 (딱 맞으면 솎지 않음)
        assertEquals(1, labelStride(count = 10, plotWidthPx = 300.0, labelWidthPx = 26.0, gapPx = 4.0))
    }

    @Test
    fun stride_just_over_slot_boundary_is_two() {
        // 라벨+gap 조금 초과 → ceil(>1)=2
        assertEquals(2, labelStride(count = 10, plotWidthPx = 300.0, labelWidthPx = 27.0, gapPx = 4.0))
    }

    @Test
    fun degenerate_inputs_do_not_thin() {
        assertEquals(1, labelStride(count = 0, plotWidthPx = 300.0, labelWidthPx = 40.0, gapPx = 4.0))
        assertEquals(1, labelStride(count = 43, plotWidthPx = 0.0, labelWidthPx = 40.0, gapPx = 4.0))
        assertEquals(1, labelStride(count = 43, plotWidthPx = 400.0, labelWidthPx = 0.0, gapPx = 4.0))
    }

    @Test
    fun default_gap_is_zero() {
        // slot = 30, 라벨 30, gap 기본 0 → ceil(1.0)=1
        assertEquals(1, labelStride(count = 10, plotWidthPx = 300.0, labelWidthPx = 30.0))
    }
}
