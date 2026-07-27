// iOS: PaceColormap.swift
package com.lumipol.graph.renderer

import com.lumipol.graph.model.BarColorRole

/**
 * 막대 하나의 색을 결정할 때 넘기는 입력(iOS `BarPaceColorInput` 대응) —
 * [ChartStyle.barColorProvider] 오버라이드 전용.
 * 기본 색 공식은 코어 [com.lumipol.graph.PaceColormap](B6)이 단독 소유한다 —
 * 렌더러 복제본(구 defaultPaceColor)은 삭제됨.
 */
data class BarPaceColorInput(
    val value: Double,      // 이 막대의 실제 페이스(sec/unit, 낮을수록 빠름)
    val fastest: Double,    // 이 런 막대들 중 최소값(가장 빠름)
    val slowest: Double,    // 이 런 막대들 중 최대값(가장 느림)
    val average: Double,    // 막대 value 평균(등거리 스플릿 → 런 평균 페이스와 일치)
    val isPartial: Boolean,
    val index: Int,
    val colorRole: BarColorRole,
)
