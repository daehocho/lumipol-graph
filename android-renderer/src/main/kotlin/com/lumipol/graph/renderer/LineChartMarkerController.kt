// iOS: RDChartView.showTouchMarker(atX:)/hideTouchMarker() — 명령형 터치 마커 요청의 Compose 대응.
package com.lumipol.graph.renderer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 프로그래매틱 터치 마커 컨트롤러 — iOS `showTouchMarker(atX:)`/`hideTouchMarker()`의 명령형 대응.
 * 외부 이벤트(구간 리스트 선택, 딥링크 등)로 특정 x에 마커를 세우고 스크럽 콜백을 받을 때 쓴다.
 * [LineChartZoomController]와 같은 요청 봉투 패턴 — 같은 x를 다시 요청해도 매 호출 적용된다.
 *
 * ### iOS 패리티
 * - [show]는 마커 생성에 성공하면 `onScrub`/`onScrubBackground`를 발화한다(제스처 스크럽과 동일 경로·
 *   동일 콜백 짝맞춤 불변식). 확대 상태에서 창 밖 x는 무시되고, 창 경계의 부동소수 오차(폭 × 1e-9)는
 *   창 안으로 클램프된다.
 * - [hide]는 마커가 표시 중이었을 때만 `onScrubEnd`를 1회 발화한다(짝 깨진 종료 통지 없음).
 * - 표시된 마커는 relayout(회전/리사이즈) 후에도 같은 x에 콜백 재발화 없이 복원된다.
 * - 차트가 아직 배치되지 않았으면(캔버스 크기 0) 요청은 조용히 무시된다(iOS `currentPlotArea == nil` 가드).
 * - **데이터 갱신 시 표시 중이던 마커는 콜백 없이 제거되고, 이전 [show] 요청은 재적용되지 않는다**
 *   (iOS `render()`가 마커를 무통지 제거하고 이전 show를 기억하지 않는 것과 동일). 갱신 후에도 마커가
 *   필요하면 호스트가 [show]를 다시 호출한다.
 */
@Stable
class LineChartMarkerController {
    /**
     * 마커 요청 봉투 — 항등성 비교로 같은 x 재요청도 스냅샷 변경으로 관측된다
     * ([LineChartZoomController.Request]와 동일 규칙). [rawX]가 null이면 숨김([hide]) 요청.
     */
    internal class Request(val rawX: Double?)

    /** 마지막 마커 요청. 요청 없음 = null. RDLineChart가 관측한다. */
    internal var request by mutableStateOf<Request?>(null)
        private set

    /** 원본 도메인 [rawX]에 마커 표시 + 스크럽 콜백 발화(iOS `showTouchMarker(atX:)`). */
    fun show(rawX: Double) {
        request = Request(rawX)
    }

    /** 마커 숨김 — 표시 중이었으면 `onScrubEnd` 1회 발화(iOS `hideTouchMarker()`). */
    fun hide() {
        request = Request(null)
    }
}
