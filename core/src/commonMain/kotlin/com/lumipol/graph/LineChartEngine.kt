package com.lumipol.graph

import com.lumipol.graph.model.*
import com.lumipol.graph.query.nearest as nearestQuery
import com.lumipol.graph.scale.AxisDomain
import com.lumipol.graph.scale.NiceScale
import com.lumipol.graph.scale.niceScale
import com.lumipol.graph.scale.yValues
import com.lumipol.graph.stats.segmentStats
import com.lumipol.graph.stats.seriesStat

object LineChartEngine {

    fun layout(data: LineChartData): LineChartLayout {
        val maxTicks = data.config.maxTicks

        // X 도메인 (모든 시리즈 점의 x) — min은 nice 경계로 내리되 max는 데이터 끝에 맞춘다.
        // 도메인 축(X)까지 nice 올림하면 데이터 뒤로 빈 구간이 생겨(예: 10.06km → 15km) 플롯 폭을 낭비한다.
        val xs = data.series.flatMap { it.points }.map { it.x }
        val xNice = niceScale(xs.minOrNull() ?: 0.0, xs.maxOrNull() ?: 1.0, maxTicks)
        val xMax = xs.maxOrNull() ?: xNice.niceMax
        val xDom = AxisDomain(xNice.niceMin, xMax)
        val xTicks = xNice.ticks.filter { it <= xMax + xNice.step * 1e-6 }

        // Y 도메인 (축별). 값이 없는 축은 null.
        val yNice: Map<Axis, NiceScale?> = Axis.entries.associateWith { axis ->
            val vals = yValues(data, axis)
            if (vals.isEmpty()) null else niceScale(vals.min(), vals.max(), maxTicks)
        }
        val yDom: Map<Axis, AxisDomain> = yNice.mapValues { (_, ns) ->
            if (ns == null) AxisDomain(0.0, 1.0) else AxisDomain(ns.niceMin, ns.niceMax)
        }

        // 시리즈 정규화
        val seriesLayout = data.series.map { s ->
            val dom = yDom.getValue(s.axis)
            SeriesLayout(
                id = s.id,
                role = s.role,
                points = s.points.map { NormalizedPoint(xDom.normalize(it.x), dom.normalize(it.y)) },
            )
        }

        // 축 tick: 어떤 출력 요소(시리즈/기준선/밴드)든 참조하는 축은 항상 여기 등장한다 —
        // refLine/refBand의 값도 yValues()에 흡수되어 해당 축의 도메인+틱을 만들어내기 때문.
        val axisTicks = buildList {
            add(AxisTicksLayout(ChartAxis.X, xTicks.map { AxisTick(it, xDom.normalize(it)) }))
            yNice[Axis.PRIMARY]?.let { ns ->
                add(AxisTicksLayout(ChartAxis.Y_PRIMARY, ns.ticks.map { AxisTick(it, yDom.getValue(Axis.PRIMARY).normalize(it)) }))
            }
            yNice[Axis.SECONDARY]?.let { ns ->
                add(AxisTicksLayout(ChartAxis.Y_SECONDARY, ns.ticks.map { AxisTick(it, yDom.getValue(Axis.SECONDARY).normalize(it)) }))
            }
        }

        // 기준선/밴드/마커
        val refLines = data.referenceLines.map {
            RefLineLayout(it.axis, yDom.getValue(it.axis).normalize(it.value), it.label)
        }
        val refBands = data.referenceBands.map {
            val dom = yDom.getValue(it.axis)
            RefBandLayout(it.axis, dom.normalize(it.lower), dom.normalize(it.upper))
        }
        val markers = data.segmentMarkers.map {
            MarkerLayout(xDom.normalize(it.x), it.label, it.emphasis)
        }

        // 통계: perSeries 전체, segments 는 첫 MAIN/PRIMARY 시리즈 기준
        val perSeries = data.series.map { seriesStat(it) }
        val splitBase = data.series.firstOrNull { it.role == SeriesRole.MAIN && it.axis == Axis.PRIMARY }
            ?: data.series.firstOrNull()
        val segments = splitBase?.let { segmentStats(it, data.config.segmentCount) } ?: emptyList()

        return LineChartLayout(
            series = seriesLayout,
            axisTicks = axisTicks,
            refLines = refLines,
            refBands = refBands,
            markers = markers,
            stats = Stats(perSeries, segments, if (segments.isEmpty()) null else splitBase?.id),
        )
    }

    /** [x]는 원시 데이터-도메인 단위(0..1 정규화 아님) — 렌더러는 터치 위치를 원시 x로 변환한 뒤 호출해야 한다. */
    fun nearest(data: LineChartData, x: Double): List<NearestResult> = nearestQuery(data, x)
}
