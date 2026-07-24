package com.lumipol.graph

/**
 * 페이스 차트 지표의 정수 id — [com.lumipol.graph.model.PaceSeriesResult.availableSeries]와
 * [SeriesSelection]의 `current`/`available`/`linePriority`가 공유하는 단일 소스.
 *
 * 코어는 여전히 도메인 프리다(라벨·단위·색은 앱 소유). 다만 두 API가 같은 정수를 주고받는데
 * 매핑을 앱마다 따로 두면 플랫폼별로 어긋나므로, 번호만 코어가 고정한다.
 */
object PaceSeriesId {
    const val PACE = 0
    const val HEART = 1
    const val CADENCE = 2
    const val ALTITUDE = 3

    /** 라인 지표 축 우선순위(앞이 주축). ALTITUDE는 배경 실루엣이라 제외. */
    val LINE_PRIORITY: List<Int> = listOf(PACE, HEART, CADENCE)
}
