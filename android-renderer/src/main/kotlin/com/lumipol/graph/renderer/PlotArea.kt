// iOS: PlotArea.swift
package com.lumipol.graph.renderer

import com.lumipol.graph.model.Axis
import com.lumipol.graph.model.NormalizedPoint

/** 플롯 영역 픽셀 좌표 한 점 (Compose `Offset` 대신 순수 primitive 표현). */
internal data class PlotPoint(val x: Double, val y: Double)

/**
 * 코어의 값-공간 정규화 좌표(0.0~1.0)를 플롯 영역 픽셀 좌표로 변환한다.
 *
 * 화면 상하 반전(예: 페이스 — 위=빠름)은 렌더러 책임이며, [invertedAxes]에 든 축만 뒤집는다.
 * arch 계약대로 **Compose 기하 타입(Offset/Rect)을 도입하지 않는다** — 생성자는 순수 크기(size)와
 * [Insets]만 받고 모든 좌표는 [Double]로 다룬다. Compose `Offset`/`Rect` 변환은 DrawScope 경계(배치2·3)에서만.
 * 뷰 bounds 원점은 항상 (0, 0)이라 원점 파라미터는 두지 않는다.
 */
internal class PlotArea(
    sizeWidth: Double,
    sizeHeight: Double,
    insets: Insets,
    val invertedAxes: Set<Axis> = emptySet(),
) {
    val minX: Double = insets.left.toDouble()
    val minY: Double = insets.top.toDouble()
    val width: Double = sizeWidth - insets.left - insets.right
    val height: Double = sizeHeight - insets.top - insets.bottom
    val maxX: Double get() = minX + width
    val maxY: Double get() = minY + height

    /**
     * 렌더 가능 여부. iOS `rect.size.width/height > 0`와 동일하게 **부호 있는** 폭·높이로 판정한다
     * — insets가 bounds보다 커서 음수 크기가 되면 렌더 불가.
     */
    val isRenderable: Boolean get() = width > 0 && height > 0

    fun x(nx: Double): Double = minX + nx * width

    fun y(ny: Double, axis: Axis): Double {
        val fractionFromTop = if (invertedAxes.contains(axis)) ny else 1.0 - ny
        return minY + fractionFromTop * height
    }

    fun point(np: NormalizedPoint, axis: Axis): PlotPoint =
        PlotPoint(x = x(np.x), y = y(np.y, axis))

    /**
     * 축 반전 여부와 무관하게 "값이 클수록 위" 규칙만 적용하는 y 변환.
     * 코어가 이미 자체 정규화한 오버레이 시리즈(축 없음) 전용 — 호스트 축의 [invertedAxes]를 무시한다.
     */
    fun yIgnoringInversion(ny: Double): Double = minY + (1.0 - ny) * height

    /** 오버레이 시리즈 전용 포인트 변환 — [yIgnoringInversion] 참고. */
    fun pointIgnoringInversion(np: NormalizedPoint): PlotPoint =
        PlotPoint(x = x(np.x), y = yIgnoringInversion(np.y))

    /** 터치 x(픽셀) → 정규화 x. 플롯 영역 밖은 0~1로 클램프. */
    fun normalizedX(px: Double): Double {
        if (width <= 0) return 0.0
        return ((px - minX) / width).coerceIn(0.0, 1.0)
    }
}
