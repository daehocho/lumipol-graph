// iOS: RDChartScrubDelegate / RDHeartRateZoneSelectionDelegate (프로토콜 → 콜백 typealias)
package com.lumipol.graph.renderer

/**
 * 차트 상호작용 콜백 계약. iOS의 `@objc` 델리게이트 프로토콜을 Kotlin 람다 typealias로 대체한다.
 *
 * **짝맞춤 불변식**(iOS `hadMarker` 가드): [OnScrubEnd]는 앞서 [OnScrub]/[OnScrubBackground]가
 * 실제로 발화됐던 경우에만 호출되어야 한다. 이 계약의 강제는 배치3의 상호작용 홀더 책임이며,
 * 배치2(도넛)는 [OnSelectSegment]만 사용한다.
 */

/** 스크럽 위치의 시리즈별 포맷 값(seriesId → 표시문자열). iOS `didScrubTo`. */
typealias OnScrub = (values: Map<String, String>) -> Unit

/** 배경 area(고도 등) 보간 실값. iOS `didScrubToBackgroundValue`. */
typealias OnScrubBackground = (value: Double) -> Unit

/** 스크럽 종료(마커가 표시 중이었을 때만). iOS `chartViewDidEndScrub`. */
typealias OnScrubEnd = () -> Unit

/**
 * 도넛 세그먼트 선택. index=null 이면 선택 해제(up/cancel). iOS `didSelectSegmentAt`.
 * **index는 원본 `data.segments` 인덱스**(value<=0 필터로 어긋난 레이아웃 인덱스가 아님).
 */
typealias OnSelectSegment = (index: Int?) -> Unit
