package com.lumipol.graph

import com.lumipol.graph.model.DonutChartData
import com.lumipol.graph.model.DonutColorRole
import com.lumipol.graph.model.DonutSegment
import kotlin.test.Test
import kotlin.test.assertEquals

/** B12/D9 — 접근성 기본 문자열 규칙(ko-KR 고정). 렌더러 하드코딩 2벌의 단일 원본. */
class ChartA11yTest {

    @Test
    fun line_chart_descriptions() {
        assertEquals("라인 차트, 데이터 없음", ChartA11y.lineChart(0, hasBackgroundArea = false))
        assertEquals("라인 차트, 배경 고도 영역", ChartA11y.lineChart(0, hasBackgroundArea = true))
        assertEquals("라인 차트, 시리즈 2개", ChartA11y.lineChart(2, hasBackgroundArea = false))
        assertEquals("라인 차트, 시리즈 3개, 배경 고도 영역 포함", ChartA11y.lineChart(3, hasBackgroundArea = true))
    }

    @Test
    fun bar_chart_descriptions() {
        assertEquals("막대 차트, 데이터 없음", ChartA11y.barChart(0, emptyList()))
        assertEquals("막대 차트, 구간 2개", ChartA11y.barChart(2, emptyList()))
        assertEquals(
            "막대 차트, 구간 2개. 구간 1 5'10\", 구간 2 5'30\"",
            ChartA11y.barChart(2, listOf("5'10\"", "5'30\"")),
        )
        // 라벨이 모자라면 빈 값은 공백 없이 낭독.
        assertEquals("막대 차트, 구간 2개. 구간 1 a, 구간 2", ChartA11y.barChart(2, listOf("a")))
    }

    @Test
    fun donut_descriptions_read_full_distribution() {
        val layout = DonutEngine.layout(
            DonutChartData(
                listOf(
                    DonutSegment(25.0, DonutColorRole.ZONE1, "워밍업"),
                    DonutSegment(75.0, DonutColorRole.ZONE2),
                ),
            ),
        )
        assertEquals("심박존 분포 도넛. 워밍업 25%, ZONE2 75%", ChartA11y.donut(layout))
        assertEquals(
            "심박존 도넛, 데이터 없음",
            ChartA11y.donut(DonutEngine.layout(DonutChartData(emptyList()))),
        )
    }

    @Test
    fun donut_selection_reads_label_and_percent() {
        assertEquals("저강도 32%", ChartA11y.donutSelection("저강도", 0.32))
        assertEquals("32%", ChartA11y.donutSelection(null, 0.316))
    }
}
