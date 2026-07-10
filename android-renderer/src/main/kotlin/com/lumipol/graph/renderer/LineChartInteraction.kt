// iOS: RDChartView.swift (상태·콜백·줌 로직 부분 — UIView 프로퍼티 뭉치 → interaction 홀더)
//
// arch 결정: iOS 뷰의 가변 프로퍼티 중 **상호작용으로 변하는 것만**(줌 창·활성 마커 rawX·제스처
// 임시창) 담는다. 데이터·스타일은 불변 composable 파라미터, 파생 layout은 [layoutForCurrentWindow].
// 이 홀더는 Compose 런타임 없이도 상태를 읽고 쓸 수 있어(스냅샷 state는 순수 JVM에서 동작) 콜백
// 짝맞춤·줌 수학을 JVM 단위테스트로 검증한다. 그리기·제스처 배선은 각각 [RDLineChart]/[LineGestures].
package com.lumipol.graph.renderer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lumipol.graph.LineChartEngine
import com.lumipol.graph.model.ChartAxis
import com.lumipol.graph.model.LineChartData
import com.lumipol.graph.model.LineChartLayout
import com.lumipol.graph.model.Point

/**
 * 라인차트 상호작용 상태 홀더 — 줌 창, 활성 마커 rawX, 제스처 임시 창을 보유하고 콜백 짝맞춤
 * 불변식을 캡슐화한다.
 *
 * @param data 불변 차트 데이터(remember 키). 시리즈 유무로 area 단독 폴백을 결정한다.
 * @param sortedArea 배경 area를 **x 오름차순 정렬**한 것(보간 이진탐색 전제). null이면 배경 없음.
 *
 * ### 콜백 짝맞춤 불변식(iOS `hadMarker` 가드)
 * - [scrubTo]는 마커 생성에 성공했을 때만 [onScrub]/[onScrubBackground]를 발화한다. 실패하면
 *   아무 콜백도 내지 않는다(짝 깨진 [onScrubEnd] 방지).
 * - [endScrub]는 **마커가 표시 중이었을 때만**([activeMarkerRawX] != null) [onScrubEnd]를 1회 발화한다.
 * - relayout(크기 변경) 시 마커는 [activeMarkerRawX]에서 draw가 재파생하므로 콜백 재발화가 구조적으로
 *   불가능하다 — iOS의 `notifyingDelegate:false` 재표시 경로를 Compose에선 파생으로 대체(아래 [scrubTo]
 *   의 `notify=false`는 그 불변식을 명시·테스트하기 위해 유지).
 */
internal class LineChartInteraction(
    val data: LineChartData,
    val sortedArea: List<Point>?,
) {
    /** 현재 줌 창(null = 전체 구간, ensureZoom 불변식). */
    var zoom by mutableStateOf<ZoomState?>(null)
        private set

    /**
     * 표시 중인 마커의 원본 도메인 x. relayout(회전/리사이즈) 후에도 같은 x에 마커를 복원하는 핵심 상태.
     * 마커 생성에 성공한 [scrubTo]에서만 세팅되고, [endScrub]에서 해제된다.
     */
    var activeMarkerRawX by mutableStateOf<Double?>(null)
        private set

    /** 최대 확대 배율(전체 구간 대비). composable이 파라미터로 주입. */
    var maxZoomScale: Double = 10.0

    // 시리즈 속성 맵(첫 시리즈 우선) — data가 불변이므로 인스턴스당 1회 계산해 스크럽(60~120Hz)
    // 마다 재구성하지 않는다. 그리기(firstWinsAxis)와 같은 규칙([firstWinsBy]) 공유.
    val axisBySeriesId = firstWinsBy(data) { it.axis }
    val roleBySeriesId = firstWinsBy(data) { it.role }

    // 콜백 — composable이 `rememberUpdatedState`로 최신 람다를 유지시킨다(stale 방지).
    var onScrub: OnScrub? = null
    var onScrubBackground: OnScrubBackground? = null
    var onScrubEnd: OnScrubEnd? = null

    // 제스처 진행 중 기준 창(드리프트 방지) — non-observable 임시값(iOS pinch/panStartWindow).
    private var pinchStartWindow: ClosedFloatingPointRange<Double>? = null
    private var panStartWindow: ClosedFloatingPointRange<Double>? = null

    // -------------------------------------------------------------------------------------
    // Layout 파생 (iOS makeFullLayout / commitViewport)
    // -------------------------------------------------------------------------------------

    /**
     * 현재 줌 창에 맞춘 layout. 확대 상태면 창 구간 windowed layout(코어 재계산), 아니면 전체 구간.
     * `xMax > xMin` 가드는 [ZoomState] 클램프가 보장하지만, 방어적으로 확대 상태만 windowed 호출한다.
     */
    fun layoutForCurrentWindow(): LineChartLayout {
        val z = zoom
        if (z != null && z.isZoomed) {
            val lo = z.window.start
            val hi = z.window.endInclusive
            if (hi > lo) return LineChartEngine.layout(data, lo, hi)
        }
        return makeFullLayout()
    }

    /**
     * 전체 구간(1x) layout. 시리즈 없이 배경 area만 있으면 X 도메인이 area x범위여야 하는데,
     * 그 규칙은 플랫폼 중립이라 코어 area-인식 오버로드가 책임진다(양 렌더러 공유).
     */
    private fun makeFullLayout(): LineChartLayout = LineChartEngine.layout(data, sortedArea)

    // -------------------------------------------------------------------------------------
    // 스크럽 (iOS showTouchMarker / hideTouchMarker)
    // -------------------------------------------------------------------------------------

    /**
     * 원본 도메인 [rawX]에 마커를 세우고(성공 시 [activeMarkerRawX] 갱신), [notify]면 콜백을 발화한다.
     *
     * 확대 상태에서 창 끝 스크럽은 도메인 역산 반올림으로 상/하한을 수 ulp 벗어날 수 있어, [TouchMarker]와
     * 동일하게 [ZoomState] 폭 × 1e-9 이내는 창 안으로 클램프한다(엄격 비교 시 끝 스크럽 침묵 드롭).
     *
     * @param context 현재 layout·plot·style·formatter를 담은 마커 컨텍스트(그리기와 동일 좌표계).
     * @return 마커가 표시되면 true.
     */
    fun scrubTo(rawX: Double, context: TouchMarkerContext, notify: Boolean): Boolean {
        var x = rawX
        val z = zoom
        if (z != null && z.isZoomed) {
            val epsilon = (z.window.endInclusive - z.window.start) * TouchMarker.WINDOW_EPSILON
            if (x < z.window.start - epsilon || x > z.window.endInclusive + epsilon) return false
            x = x.coerceIn(z.window.start, z.window.endInclusive)
        }

        val hadMarker = activeMarkerRawX != null
        // 시리즈 없는 차트(배경 area 단독)는 스냅 격자가 없으므로 전용 마커로 폴백. 시리즈가 있으면
        // area가 있어도 스냅 계약 유지(스냅 실패 시 무마커 — 배경 단독 폴백 안 함).
        val result = if (data.series.isEmpty()) {
            if (sortedArea?.isEmpty() == false) TouchMarker.makeBackgroundOnly(x, context) else null
        } else {
            TouchMarker.make(x, context)
        }

        if (result == null) {
            // 표시 중이던 마커가 사라질 때만 종료 통지 — 짝 깨진 endScrub 방지(hideTouchMarker와 동일 계약).
            if (notify && hadMarker) {
                activeMarkerRawX = null
                onScrubEnd?.invoke()
            }
            return false
        }

        activeMarkerRawX = x
        if (notify) {
            onScrub?.invoke(result.valuesBySeriesId)
            // 배경 area 보간은 코어 질의(interpolatedY) — 양 플랫폼 렌더러 공유(0.9.0 이관).
            sortedArea?.let { area ->
                LineChartEngine.interpolatedY(area, result.snappedX)?.let { value ->
                    onScrubBackground?.invoke(value)
                }
            }
        }
        return true
    }

    /** 스크럽 종료: 마커가 표시 중이었으면 [onScrubEnd]를 1회 발화하고 [activeMarkerRawX]를 해제. */
    fun endScrub() {
        if (activeMarkerRawX != null) {
            activeMarkerRawX = null
            onScrubEnd?.invoke()
        }
    }

    // -------------------------------------------------------------------------------------
    // 줌 (iOS ensureZoomState / zoom(toXRange:) / resetZoom / pinch / handleZoomedPan)
    // -------------------------------------------------------------------------------------

    /** 프로그래매틱 줌 — 창 설정 후 마커를 숨긴다(iOS `zoom(toXRange:)`). 범위가 퇴화면 무시. */
    fun zoomToRange(range: ClosedFloatingPointRange<Double>) {
        if (range.endInclusive <= range.start) return
        ensureZoom()
        zoom = zoom?.setWindow(range)
        // '비줌 = null' 불변식 유지 — 전체 도메인 창이 non-null로 남으면(zoomed-but-full)
        // pinchEnded/panEnded와 상태 표현이 어긋난다.
        if (zoom?.isZoomed != true) zoom = null
        endScrub()
    }

    /** 전체 구간으로 복귀(iOS `resetZoom`). 이미 전체면 무시. */
    fun resetZoom() {
        if (zoom == null) return
        zoom = null
    }

    /** 라이브 핀치 시작 — 줌 상태 보장 + 마커 숨김 + 기준 창 스냅샷(iOS `pinchBegan`). */
    fun pinchBegan() {
        ensureZoom()
        endScrub()
        pinchStartWindow = zoom?.window
    }

    /** 라이브 핀치 진행 — 기준 창에서 누적 배율·앵커로 재계산(iOS `pinchChanged`). */
    fun pinchChanged(cumulativeScale: Double, anchor: Double) {
        val start = pinchStartWindow ?: return
        zoom = zoom?.pinch(start, cumulativeScale, anchor, maxZoomScale)
    }

    /** 라이브 핀치 종료 — 기준 창 해제, 전체 구간에서 끝났으면 줌 상태 정리(iOS `pinchEnded`). */
    fun pinchEnded() {
        pinchStartWindow = null
        if (zoom?.isZoomed != true) zoom = null
    }

    /** 확대 팬 시작 — 마커 숨김 + 기준 창 스냅샷(iOS `handleZoomedPan .began`). */
    fun panBegan() {
        endScrub()
        panStartWindow = zoom?.window
    }

    /**
     * 확대 팬 진행 — 플롯 폭 대비 누적 이동 비율([fraction], 오른쪽 드래그=+)만큼 기준 창을 왼쪽 이동.
     * 기준 창에 누적 이동을 적용해 드리프트를 막는다(iOS `handleZoomedPan .changed`).
     */
    fun panChanged(fraction: Double) {
        val start = panStartWindow ?: return
        val span = start.endInclusive - start.start
        val targetLower = start.start - fraction * span
        zoom = zoom?.setWindow(targetLower..(targetLower + span))
    }

    /** 확대 팬 종료 — 기준 창 해제, 전체 구간에서 끝났으면 줌 상태 정리(iOS `handleZoomedPan .ended`). */
    fun panEnded() {
        panStartWindow = null
        if (zoom?.isZoomed != true) zoom = null
    }

    /** 라이브 제스처(핀치/팬)가 진행 중인가 — consume·상태 정리 판단용. */
    val isGestureActive: Boolean get() = pinchStartWindow != null || panStartWindow != null

    /**
     * 현재 전체 layout의 X tick 양끝으로 줌 상태를 초기화(iOS `ensureZoomState`).
     * zoom==null(전체 구간) 불변식에서만 유효하며, tick 부족·퇴화 도메인이면 초기화하지 않는다.
     */
    private fun ensureZoom() {
        if (zoom != null) return
        val xTicks = makeFullLayout().ticksFor(ChartAxis.X) ?: return
        val scale = AxisScale.from(xTicks) ?: return
        val lower = scale.value(0.0)
        val upper = scale.value(1.0)
        if (upper <= lower) return
        zoom = ZoomState.full(lower..upper)
    }
}
