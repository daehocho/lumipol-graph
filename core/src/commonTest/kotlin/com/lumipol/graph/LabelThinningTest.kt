package com.lumipol.graph

import com.lumipol.graph.query.isLabelVisible
import com.lumipol.graph.query.labelStride
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    // isLabelVisible: 첫·마지막은 항상 표시, 마지막 근처 배수는 숨겨 간격 유지(리뷰 #1).
    @Test
    fun visibility_always_shows_first_and_last() {
        // count=43(0..42), stride=5 → lastMultiple=40, 42와 거리 2<5 → 40 숨김
        assertTrue(isLabelVisible(0, count = 43, stride = 5))
        assertTrue(isLabelVisible(42, count = 43, stride = 5))
        assertTrue(isLabelVisible(35, count = 43, stride = 5))
        assertFalse(isLabelVisible(40, count = 43, stride = 5)) // 마지막과 붙어 숨김
        assertFalse(isLabelVisible(41, count = 43, stride = 5)) // 배수 아님
        assertFalse(isLabelVisible(37, count = 43, stride = 5)) // 배수 아님
    }

    @Test
    fun visibility_stride_one_shows_all() {
        for (i in 0 until 10) assertTrue(isLabelVisible(i, count = 10, stride = 1))
    }

    @Test
    fun visibility_single_bar_is_shown() {
        assertTrue(isLabelVisible(0, count = 1, stride = 5))
    }

    @Test
    fun visibility_aligned_end_keeps_multiple() {
        // count=41(0..40), stride=5 → 마지막 40이 배수이자 끝 → 표시, 35도 표시
        assertTrue(isLabelVisible(40, count = 41, stride = 5))
        assertTrue(isLabelVisible(35, count = 41, stride = 5))
    }

    // 마지막 라벨 우측 클램프 반영 stride(0.49.0). 클램프는 라벨을 (lastW-slot)/2만큼 왼쪽으로
    // 밀어 stride가 확보해 둔 gap 예산을 먹는다 — 그만큼 폭에 얹어 계산한다.
    @Test
    fun clamp_aware_stride_widens_when_last_label_overflows_slot() {
        // 9막대·plot 232 → slot 25.78. 라벨 44 + gap 6 → 구 stride 2(간격 예산 7.56px)인데
        // 클램프 이동량 9.11px가 그걸 넘어 직전 라벨과 1.55px 겹쳤다. 이제 3.
        assertEquals(2, labelStride(count = 9, plotWidthPx = 232.0, labelWidthPx = 44.0, gapPx = 6.0))
        assertEquals(
            3,
            labelStride(count = 9, plotWidthPx = 232.0, labelWidthPx = 44.0, gapPx = 6.0, lastLabelWidthPx = 44.0),
        )
    }

    @Test
    fun clamp_aware_stride_matches_plain_when_last_label_fits_slot() {
        // 마지막 라벨이 슬롯 이하면 렌더러가 클램프하지 않으므로 종전 stride와 동일해야 한다
        // (넓은 중간 라벨만으로 라벨이 더 솎이면 안 된다).
        val slot = 232.0 / 9 // 25.78
        assertEquals(
            labelStride(count = 9, plotWidthPx = 232.0, labelWidthPx = 44.0, gapPx = 6.0),
            labelStride(count = 9, plotWidthPx = 232.0, labelWidthPx = 44.0, gapPx = 6.0, lastLabelWidthPx = slot),
        )
        assertEquals(
            labelStride(count = 9, plotWidthPx = 232.0, labelWidthPx = 44.0, gapPx = 6.0),
            labelStride(count = 9, plotWidthPx = 232.0, labelWidthPx = 44.0, gapPx = 6.0, lastLabelWidthPx = 0.0),
        )
    }

    @Test
    fun clamp_aware_stride_degenerate_inputs_do_not_thin() {
        assertEquals(1, labelStride(0, 300.0, 40.0, 4.0, 40.0))
        assertEquals(1, labelStride(43, 0.0, 40.0, 4.0, 40.0))
        assertEquals(1, labelStride(43, 400.0, 0.0, 4.0, 40.0))
    }

    @Test
    fun clamp_aware_stride_leaves_no_overlap_with_previous_visible_label() {
        // 리뷰 실측 겹침 조합(n=9/15/19/28 …)을 포함한 전 구간 스캔. 마지막 라벨은 우측 클램프로
        // 왼쪽 끝이 plot - lastW가 되고, 직전 표시 라벨은 중앙 정렬 상태다. 둘 사이 간격이 gap
        // 이상이어야 한다. count <= stride 조합(첫·마지막 강제 표시로 stride가 성립하지 않는
        // 기존 한계, KDoc 명시)만 제외한다.
        val gap = 6.0
        for (n in 2..45) {
            for (plot in listOf(232.0, 300.0, 360.0)) {
                for (w in listOf(20.0, 30.0, 36.0, 44.0, 62.0, 88.0)) {
                    val slot = plot / n
                    val stride = labelStride(n, plot, w, gap, lastLabelWidthPx = w)
                    if (n - 1 <= stride) continue
                    val prev = (n - 2 downTo 0).first { isLabelVisible(it, n, stride) }
                    val prevRight = slot * prev + slot / 2 + w / 2
                    val lastLeft = if (w > slot) plot - w else slot * (n - 1) + slot / 2 - w / 2
                    assertTrue(
                        lastLeft - prevRight >= gap - 1e-9,
                        "overlap n=$n plot=$plot w=$w stride=$stride prev=$prev gap=${lastLeft - prevRight}",
                    )
                }
            }
        }
    }
}
