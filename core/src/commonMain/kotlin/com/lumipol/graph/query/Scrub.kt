package com.lumipol.graph.query

import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.LineChartLayout
import com.lumipol.graph.model.ScrubPoint
import com.lumipol.graph.model.ScrubResult
import com.lumipol.graph.model.SeriesRole
import kotlin.math.abs

/**
 * 표시 창 경계의 부동소수 흡수 epsilon — 도메인 양끝 값은 반올림으로 0/1을 수 ulp 벗어날 수
 * 있어, 이 범위는 창 안으로 간주해 클램프한다. 엄격 비교로 바꾸면 끝-탭이 침묵 드롭되는 회귀
 * (양 렌더러 실측값 1e-9의 코어 승격 — 렌더러 수치 리터럴 금지, 경계 정책 §4-3).
 */
const val SCRUB_WINDOW_EPSILON: Double = 1e-9

/**
 * 스크럽 근접 질의 — 창 필터·스냅 소스 선택·정규화 좌표 산출을 코어가 확정한다(B2).
 *
 * 규칙(구 렌더러 TouchMarker 로직의 코어 승격, 동작 불변):
 * - 창은 [layout] X 도메인 ± [SCRUB_WINDOW_EPSILON] — 창 밖 전역 최근접점이 창 안 이웃을
 *   가리는 것을 막는다(줌 가장자리 스크럽).
 * - 스냅 소스는 main 시리즈 근접점(없으면 첫 시리즈) — 오버레이는 성긴 샘플일 수 있다.
 * - 시리즈별 근접점이 창 밖이면 그 시리즈는 결과에서 제외(짧은 보조 시리즈가 창 밖 값을
 *   스크럽 위치인 양 보고하는 것을 방지).
 * - 오버레이 ny는 layout의 자체 정규화 포인트 중 근접점의 y. layout에 없으면 null(값만 전달).
 * - 축 시리즈인데 해당 축 도메인이 없으면 제외(도트·값 모두 불능).
 *
 * @return 표시 불가(축퇴 도메인·근접점 없음/전부 창밖)면 null.
 */
fun nearestScrub(data: LineChartData, layout: LineChartLayout, x: Double): ScrubResult? {
    val xDomain = layout.domains.x
    if (xDomain.max <= xDomain.min) return null

    val results = nearest(
        data,
        x,
        xMin = xDomain.denormalize(-SCRUB_WINDOW_EPSILON),
        xMax = xDomain.denormalize(1 + SCRUB_WINDOW_EPSILON),
    )
    if (results.isEmpty()) return null

    // 첫 시리즈 우선 속성 맵 — 코어 API가 id 유일성을 강제하지 않으므로 중복 시 첫 정의를 따른다.
    val roleById = HashMap<String, SeriesRole>()
    val axisById = HashMap<String, Axis>()
    for (s in data.series) {
        if (s.id !in roleById) {
            roleById[s.id] = s.role
            axisById[s.id] = s.axis
        }
    }

    val snapSource = results.firstOrNull { roleById[it.seriesId] == SeriesRole.MAIN } ?: results.first()
    val snappedX = snapSource.x
    val rawNx = xDomain.normalize(snappedX)
    if (rawNx < -SCRUB_WINDOW_EPSILON || rawNx > 1 + SCRUB_WINDOW_EPSILON) return null
    val nx = rawNx.coerceIn(0.0, 1.0)

    val perSeries = buildList {
        for (r in results) {
            val seriesNx = xDomain.normalize(r.x)
            if (seriesNx < -SCRUB_WINDOW_EPSILON || seriesNx > 1 + SCRUB_WINDOW_EPSILON) continue
            val role = roleById[r.seriesId] ?: SeriesRole.MAIN
            if (role == SeriesRole.OVERLAY) {
                val overlayNy = layout.series
                    .firstOrNull { it.id == r.seriesId && it.role == SeriesRole.OVERLAY }
                    ?.points?.minByOrNull { abs(it.x - seriesNx) }?.y
                add(ScrubPoint(r.seriesId, r.x, r.y, nx, overlayNy, role, Axis.PRIMARY, ChartAxis.Y_OVERLAY))
                continue
            }
            val axis = axisById[r.seriesId] ?: continue
            val yDomain = when (axis) {
                Axis.PRIMARY -> layout.domains.yPrimary
                Axis.SECONDARY -> layout.domains.ySecondary
            } ?: continue
            val chartAxis = when (axis) {
                Axis.PRIMARY -> ChartAxis.Y_PRIMARY
                Axis.SECONDARY -> ChartAxis.Y_SECONDARY
            }
            add(ScrubPoint(r.seriesId, r.x, r.y, nx, yDomain.normalize(r.y), role, axis, chartAxis))
        }
    }
    if (perSeries.isEmpty()) return null
    return ScrubResult(snappedX, nx, snapSource.seriesId, perSeries)
}
