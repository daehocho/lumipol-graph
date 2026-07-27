package com.lumipol.graph

import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutChartLayout
import com.lumipol.graph.model.DonutSegmentLayout
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/** 도넛 세그먼트 각도(fraction) 계산. value<=0 세그먼트는 제외. 순수 함수. */
object DonutEngine {

    /**
     * 최소 터치 타겟(dp/pt) — WCAG/Material 권장. 얇은 링에서도 이만큼의 히트 대역을 확보한다.
     * D7 결정(44 문서): AOS의 관대한 대역을 SDK 기본으로 — 렌더러는
     * `hitBand = max(ringWidth, MIN_HIT_TARGET_DP × density)`로 대역을 만든 뒤 비율로 환산한다.
     */
    const val MIN_HIT_TARGET_DP: Double = 48.0
    fun layout(data: DonutChartData): DonutChartLayout {
        val valid = data.segments.withIndex().filter { it.value.value > 0.0 }
        val total = valid.sumOf { it.value.value }
        if (valid.isEmpty() || total <= 0.0) {
            return DonutChartLayout(emptyList(), 0.0)
        }
        var cursor = 0.0
        val out = valid.map { (index, seg) ->
            val sweep = seg.value / total
            val layout = DonutSegmentLayout(
                startFraction = cursor,
                sweepFraction = sweep,
                value = seg.value,
                colorRole = seg.colorRole,
                sourceIndex = index,
                label = seg.label,
            )
            cursor += sweep
            layout
        }
        return DonutChartLayout(out, total)
    }

    /**
     * 탭 토글 전이(0.26.0). tapped=null(링 밖·구멍)→해제, 같은 조각 재탭→해제, 다른 조각→선택 이동.
     * 타이머·통지는 플랫폼 관심사 — 여기서는 다음 선택 인덱스만 결정한다.
     */
    fun toggleSelection(current: Int?, tapped: Int?): Int? = when {
        tapped == null -> null
        tapped == current -> null
        else -> tapped
    }

    /**
     * 도넛 히트테스트(B4) — **비율 공간**(px 무관): 입력은 링 중심 반경 r로 나눈 값이다.
     * `dxRatio=(px−cx)/r`, `dyRatio=(py−cy)/r`, `hitBandRatio=판정 대역 폭/r`.
     * 링 폭 인자는 불필요 — 링 폭은 픽셀 공간에서 r을 정하는 데만 쓰였고 비율 공간에선 r=1이다.
     *
     * 규칙(구 렌더러 2벌의 동작 그대로):
     * - 반경 검사(1 ± hitBandRatio/2 대역 밖 → null)로 도넛 구멍·모서리 탭의 허위 선택을 막는다.
     * - 각도는 12시 기준 시계방향 fraction. 조각 매칭은 `[start, start+sweep)`.
     * - 반환은 **원본 `DonutChartData.segments` 인덱스**([DonutSegmentLayout.sourceIndex]) —
     *   value<=0 필터 규칙을 소비자가 복제하지 않는다. 매칭 없으면 null.
     *
     * libm 주의(경계 정책 §5): [atan2]를 쓰지만 출력은 이산 인덱스뿐이라 JVM/Native ULP 차이가
     * 조각 경계 정확히 그 지점(측도 0)에서만 관측 가능 — 골든은 인덱스만 기록하므로 결정론 무해.
     */
    fun hitTest(
        dxRatio: Double,
        dyRatio: Double,
        hitBandRatio: Double,
        layout: DonutChartLayout,
    ): Int? {
        if (layout.total <= 0.0) return null
        val distance = sqrt(dxRatio * dxRatio + dyRatio * dyRatio)
        val halfBand = hitBandRatio / 2
        if (distance < 1 - halfBand || distance > 1 + halfBand) return null

        var angle = atan2(dyRatio, dxRatio) + PI / 2 // 12시 기준
        if (angle < 0) angle += 2 * PI
        val frac = angle / (2 * PI)

        val segment = layout.segments.firstOrNull {
            frac >= it.startFraction && frac < it.startFraction + it.sweepFraction
        } ?: return null
        return segment.sourceIndex.takeIf { it >= 0 }
    }
}
