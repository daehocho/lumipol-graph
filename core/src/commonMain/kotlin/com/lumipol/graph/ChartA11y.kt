package com.lumipol.graph

import com.lumipol.graph.model.DonutChartLayout
import kotlin.math.roundToInt

/**
 * 접근성 낭독 문자열 기본 규칙(B12, D9) — ko-KR 고정(05 승인).
 *
 * 렌더러는 표시 문자열을 만들지 않는다(경계 정책 §4-4) — 이 기본 문자열을 배치하거나, 앱이
 * 주입한 문자열을 배치한다. 번역·브랜드 표현이 필요한 앱은 각 차트의 override 인자로 통째
 * 주입한다(문자열 리소스는 앱 소유).
 *
 * 낭독 범위는 D9 결정(44 문서)대로 AOS안: 3차트 모두 + 도넛은 전체 분포.
 */
object ChartA11y {

    /** 라인 차트 요약 — 시리즈 수·배경 고도 영역 유무. */
    fun lineChart(seriesCount: Int, hasBackgroundArea: Boolean): String = when {
        seriesCount == 0 && !hasBackgroundArea -> "라인 차트, 데이터 없음"
        seriesCount == 0 -> "라인 차트, 배경 고도 영역"
        else -> "라인 차트, 시리즈 ${seriesCount}개" + if (hasBackgroundArea) ", 배경 고도 영역 포함" else ""
    }

    /** 막대 차트 요약 — 구간 수, 라벨이 있으면 구간별 값도 낭독. */
    fun barChart(barCount: Int, barLabels: List<String>): String {
        if (barCount <= 0) return "막대 차트, 데이터 없음"
        val detail = barLabels.takeIf { it.isNotEmpty() }?.let { labels ->
            (0 until barCount).joinToString(", ") { i ->
                "구간 ${i + 1} ${labels.getOrNull(i).orEmpty()}".trim()
            }
        }
        return "막대 차트, 구간 ${barCount}개" + (detail?.let { ". $it" } ?: "")
    }

    /** 심박존 도넛 요약 — 전체 분포(존별 퍼센트) 낭독(D9). 라벨 없으면 colorRole 이름. */
    fun donut(layout: DonutChartLayout): String {
        if (layout.total <= 0.0 || layout.segments.isEmpty()) return "심박존 도넛, 데이터 없음"
        val zones = layout.segments.joinToString(", ") { seg ->
            "${seg.label ?: seg.colorRole.name} ${percent(seg.sweepFraction)}%"
        }
        return "심박존 분포 도넛. $zones"
    }

    /** 도넛 선택 조각 낭독 — "라벨 N%" (라벨 없으면 "N%"). */
    fun donutSelection(label: String?, sweepFraction: Double): String =
        listOfNotNull(label, "${percent(sweepFraction)}%").joinToString(" ")

    private fun percent(fraction: Double): Int = (fraction * 100).roundToInt()
}
