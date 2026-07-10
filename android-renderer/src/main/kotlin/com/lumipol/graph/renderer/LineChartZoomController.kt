// iOS: RDChartView.zoom(toXRange:) — 명령형 줌 요청의 Compose 대응.
package com.lumipol.graph.renderer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 프로그래매틱 줌 컨트롤러 — iOS `zoom(toXRange:)`의 명령형 대응.
 *
 * [RDLineChart.zoomXRange]는 선언형 경로라 **값이 바뀔 때만** 반응한다(같은 구간 재요청은
 * 리컴포지션에 잡히지 않음). 사용자가 제스처로 벗어난 뒤 같은 구간을 다시 요청하는 UX
 * ("랩 2로 이동" 버튼 재탭)는 이 컨트롤러의 [zoomTo]로 — 같은 구간이어도 매 호출 적용된다.
 */
@Stable
class LineChartZoomController {
    /**
     * 줌 요청 봉투 — 일부러 equals를 정의하지 않는다(항등성 비교). 같은 구간이라도 [zoomTo]
     * 호출마다 새 인스턴스가 되어 스냅샷 변경으로 관측된다(값으로 저장하면 재요청이 무시됨).
     */
    internal class Request(val range: ClosedFloatingPointRange<Double>)

    /** 마지막 줌 요청. 요청 없음 = null. RDLineChart가 관측한다. */
    internal var request by mutableStateOf<Request?>(null)
        private set

    /** [range] 구간으로 확대 요청. 같은 구간을 다시 요청해도 매번 적용된다. */
    fun zoomTo(range: ClosedRange<Double>) {
        request = Request(range.start..range.endInclusive)
    }
}
