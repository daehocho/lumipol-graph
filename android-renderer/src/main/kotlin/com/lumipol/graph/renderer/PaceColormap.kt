// iOS: PaceColormap.swift
package com.lumipol.graph.renderer

import androidx.compose.ui.graphics.Color
import com.lumipol.graph.model.BarColorRole

/** 막대 하나의 색을 결정할 때 넘기는 입력(iOS `BarPaceColorInput` 대응). */
data class BarPaceColorInput(
    val value: Double,      // 이 막대의 실제 페이스(sec/unit, 낮을수록 빠름)
    val fastest: Double,    // 이 런 막대들 중 최소값(가장 빠름)
    val slowest: Double,    // 이 런 막대들 중 최대값(가장 느림)
    val average: Double,    // 막대 value 평균(등거리 스플릿 → 런 평균 페이스와 일치)
    val isPartial: Boolean,
    val index: Int,
    val colorRole: BarColorRole,
)

/**
 * 기본 연속 팔레트 — 지도 페이스 색 비색약 3구간 공식 이식(iOS `ChartStyle.defaultPaceColor`).
 * 빠름(값 낮음) 파랑↔청록, 평균 근처 초록↔노랑, 느림(값 높음) 노랑↔빨강.
 * average 앵커로 pace1/pace2를 잡아 평균 근처에 색 해상도를 더 준다.
 * fastest==slowest 등 구간 길이 0 축퇴는 중간 초록으로 폴백.
 */
fun ChartStyle.Companion.defaultPaceColor(input: BarPaceColorInput): Color {
    val f = input.fastest; val s = input.slowest; val a = input.average; val p = input.value
    if (s <= f) return Color(red = 0f, green = 1f, blue = 0f)
    val pace1 = a - (a - f) * 0.70
    val pace2 = a + (s - a) * 0.25
    val length1 = pace1 - f; val length2 = pace2 - pace1; val length3 = s - pace2
    fun clamp(x: Double): Float = x.coerceIn(0.0, 1.0).toFloat()
    return when {
        p < pace1 -> {                                  // 파랑↔청록
            val cv = if (length1 > 0) clamp((pace1 - maxOf(f, p)) / length1) else 0f
            Color(red = 0f, green = 1f - 0.4f * cv, blue = 1f)
        }
        p < pace2 -> {                                  // 초록↔노랑
            val cv = if (length2 > 0) clamp((pace2 - p) / length2) else 0f
            Color(red = 1f - cv, green = 1f, blue = 0f)
        }
        else -> {                                       // 노랑↔빨강
            val cv = if (length3 > 0) clamp((s - minOf(s, p)) / length3) else 0f
            Color(red = 1f, green = cv, blue = 0f)
        }
    }
}
