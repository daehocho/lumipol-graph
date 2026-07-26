package com.lumipol.graph

/**
 * 페이스 차트 지표의 정수 id — [com.lumipol.graph.model.PaceSeriesResult.availableSeries]와
 * [SeriesSelection]의 `current`/`available`/`priority`가 공유하는 단일 소스.
 *
 * 코어는 여전히 도메인 프리다(라벨·단위·색은 앱 소유). 다만 두 API가 같은 정수를 주고받는데
 * 매핑을 앱마다 따로 두면 플랫폼별로 어긋나므로, 번호만 코어가 고정한다.
 */
object PaceSeriesId {
    const val PACE = 0
    const val HEART = 1
    const val CADENCE = 2
    const val ALTITUDE = 3

    /**
     * 라인 지표 축 우선순위(앞이 주축) — [SeriesSelection.assignSlots] 전용.
     * ALTITUDE는 배경 실루엣(축 슬롯 없음)이라 제외한다.
     *
     * [SeriesSelection.normalized]의 `priority`로 넘기면 안 된다 — 고도만 가용한 기록에서
     * 폴백이 실패해 빈 선택("데이터 없음")이 된다. 그 용도는 [DISPLAY_PRIORITY].
     */
    val LINE_PRIORITY: List<Int> = listOf(PACE, HEART, CADENCE)

    /**
     * 고도 포함 전체 표시 우선순위 — [SeriesSelection.normalized]의 `priority` 계약
     * ("라인만이 아니라 고도 포함")을 충족하는 코어 상수. 고도만 측정된 기록도
     * 폴백으로 살아남는다. 축 슬롯 배정([SeriesSelection.assignSlots])에는 [LINE_PRIORITY].
     */
    val DISPLAY_PRIORITY: List<Int> = LINE_PRIORITY + ALTITUDE
}
