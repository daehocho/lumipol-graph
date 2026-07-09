package com.lumipol.graph.query

import com.lumipol.graph.model.Point

/**
 * 배경 area(예: 고도 실루엣)의 도메인 x 위치 y를 선형 보간. 범위 밖은 양 끝값으로 클램프,
 * 빈 리스트는 null. [points]는 x 오름차순 전제(렌더러가 저장 시 정렬) — 스크럽은 60~120Hz로
 * 호출되므로 수천 포인트 선형 탐색 대신 이진 탐색으로 브래킷 구간을 찾는다.
 */
fun interpolatedY(points: List<Point>, x: Double): Double? {
    val first = points.firstOrNull() ?: return null
    val last = points.last()
    if (x <= first.x) return first.y
    if (x >= last.x) return last.y
    // x <= points[i].x 를 만족하는 최소 i (선형 탐색과 동일한 구간 선택)
    var low = 1
    var high = points.lastIndex
    while (low < high) {
        val mid = (low + high) / 2
        if (x <= points[mid].x) high = mid else low = mid + 1
    }
    val p0 = points[low - 1]
    val p1 = points[low]
    val dx = p1.x - p0.x
    val t = if (dx == 0.0) 0.0 else (x - p0.x) / dx
    return p0.y + t * (p1.y - p0.y)
}
