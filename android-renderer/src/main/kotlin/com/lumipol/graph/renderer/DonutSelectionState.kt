// iOS: RDHeartRateZoneView.selectSegment(at:)
//
// 도넛 탭 선택 상태(0.27.0). 레전드처럼 차트 밖 UI가 같은 선택을 읽고 구동해야 할 때,
// 앱이 [rememberDonutSelectionState]로 만들어 차트와 공유한다. 넘기지 않으면 차트가
// 같은 규칙의 내부 기본 홀더를 쓰므로 기존 호출자의 동작은 그대로다.
package com.lumipol.graph.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lumipol.graph.DonutEngine
import com.lumipol.graph.model.DonutChartData

@Stable
class DonutSelectionState internal constructor(
    initial: Int?,
    private val selectableIndices: Set<Int>,
) {
    /** 선택된 **원본 data.segments 인덱스**. null=선택 없음. 쓰기는 [toggle]로만. */
    var selectedIndex: Int? by mutableStateOf(initial)
        internal set

    /**
     * 탭 토글 전이를 적용한다(코어 규칙 재사용) — 같은 인덱스면 해제, null이면 해제, 다르면 이동.
     * 레이아웃에 없는 인덱스(범위 밖, value<=0으로 필터된 세그먼트)는 무시한다(상태 불변·통지
     * 없음 — iOS `layoutContainsSegment` 패리티). 햅틱·통지는 호출부 몫이다(컴포지션 로컬을
     * 홀더가 소유할 수 없다 — iOS는 메서드가 대신 울린다).
     */
    fun toggle(index: Int?) {
        if (index != null && index !in selectableIndices) return
        selectedIndex = DonutEngine.toggleSelection(selectedIndex, index)
    }
}

/** [data]가 바뀌면 선택을 리셋하는 홀더 — 차트 기본값과 같은 규칙. */
@Composable
fun rememberDonutSelectionState(data: DonutChartData): DonutSelectionState =
    remember(data) {
        DonutSelectionState(null, DonutEngine.layout(data).segments.map { it.sourceIndex }.toSet())
    }
