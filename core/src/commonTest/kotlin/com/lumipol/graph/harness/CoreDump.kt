package com.lumipol.graph.harness

import com.lumipol.graph.BarChartEngine
import com.lumipol.graph.DonutEngine
import com.lumipol.graph.HeartRateZoneEngine
import com.lumipol.graph.LineChartEngine
import com.lumipol.graph.PaceSeriesEngine
import com.lumipol.graph.PaceSeriesId
import com.lumipol.graph.SeriesSelection
import com.lumipol.graph.model.*
import com.lumipol.graph.query.barIndexAtX
import com.lumipol.graph.query.heightFractions
import com.lumipol.graph.query.isLabelVisible
import com.lumipol.graph.query.labelStride
import com.lumipol.graph.scale.Y_AXIS_HEADROOM_FRACTION
import com.lumipol.graph.scale.niceScale

/**
 * 0.7단계 코어 덤프 — 고정 입력(HarnessFixtures) 전체에 대해 코어가 산출하는 중간 결과를
 * 결정론적 JSON으로 조립한다. JVM/iosSimulatorArm64 양쪽에서 같은 문자열이 나와야 하며,
 * diff는 scripts/diff-core-dump.py 가 수행한다(T1 완전 일치 / T2 허용 오차 — 05 문서 기준).
 */
object CoreDump {

    fun build(): String {
        val sections = listOf(
            section("meta", listOf(
                "schema" to jint(1),
                "coreVersion" to jstr("0.29.0"),
            )),
            section("constants", listOf(
                "Y_AXIS_HEADROOM_FRACTION" to jnum(Y_AXIS_HEADROOM_FRACTION),
                "PaceSeriesId" to jobj(
                    "PACE" to jint(PaceSeriesId.PACE),
                    "HEART" to jint(PaceSeriesId.HEART),
                    "CADENCE" to jint(PaceSeriesId.CADENCE),
                    "ALTITUDE" to jint(PaceSeriesId.ALTITUDE),
                ),
                "LINE_PRIORITY" to jarr(PaceSeriesId.LINE_PRIORITY.map { jint(it) }),
                "DISPLAY_PRIORITY" to jarr(PaceSeriesId.DISPLAY_PRIORITY.map { jint(it) }),
            )),
            section("niceScale", HarnessFixtures.niceScaleCases.map { (name, a) ->
                val ns = niceScale(a[0], a[1], a[2].toInt(), a[3])
                name to jobj(
                    "niceMin" to jnum(ns.niceMin),
                    "niceMax" to jnum(ns.niceMax),
                    "step" to jnum(ns.step),
                    "tickCount" to jint(ns.ticks.size),
                    "ticks" to jarr(ns.ticks.map { jnum(it) }),
                )
            }),
            section("lineChart", HarnessFixtures.lineCases.map { (name, data) ->
                name to renderLineLayout(LineChartEngine.layout(data))
            }),
            section("lineChartWindowed", HarnessFixtures.windowedCases.map { (name, w) ->
                val (data, xMin, xMax) = w
                name to renderLineLayout(LineChartEngine.layout(data, xMin, xMax))
            }),
            section("lineChartBackgroundOnly", HarnessFixtures.backgroundOnlyCases.map { (name, c) ->
                name to renderLineLayout(LineChartEngine.layout(c.first, c.second))
            }),
            section("nearest", buildList {
                val l09 = HarnessFixtures.lineCases.first { it.first == "L09_pace_hr_markers" }.second
                HarnessFixtures.nearestXs.forEach { x ->
                    add("x_${jnum(x)}" to jarr(LineChartEngine.nearest(l09, x).map { renderNearest(it) }))
                }
                add("windowed_1.0_2.0_x_2.4" to jarr(LineChartEngine.nearest(l09, 2.4, 1.0, 2.0).map { renderNearest(it) }))
            }),
            section("interpolatedY", HarnessFixtures.interpolationCases.flatMap { (name, points, xs) ->
                xs.map { x -> "${name}_x_${jnum(x)}" to jnum(LineChartEngine.interpolatedY(points, x)) }
            }),
            section("barChart", HarnessFixtures.barCases.map { (name, data) ->
                name to renderBarLayout(BarChartEngine.layout(data))
            }),
            section("timeBucket", HarnessFixtures.timeBucketCases.map { sec ->
                "running_${jnum(sec)}" to jnum(BarChartEngine.chooseTimeBucketSeconds(sec))
            }),
            section("donut", HarnessFixtures.donutCases.map { (name, data) ->
                name to renderDonutLayout(DonutEngine.layout(data))
            }),
            section("donutToggle", HarnessFixtures.donutToggleCases.map { (cur, tap) ->
                "cur_${cur ?: "null"}_tap_${tap ?: "null"}" to (DonutEngine.toggleSelection(cur, tap)?.let { jint(it) } ?: "null")
            }),
            section("heartRateZone", HarnessFixtures.hrZoneMaxHrCases.map { maxHr ->
                "maxHr_$maxHr" to jarr(HeartRateZoneEngine.calculate(HarnessFixtures.hrZoneSamples, maxHr).map { jnum(it) })
            }),
            section("zoneBpmRanges", HarnessFixtures.hrZoneMaxHrCases.map { maxHr ->
                "maxHr_$maxHr" to jarr(HeartRateZoneEngine.zoneBpmRanges(maxHr).map {
                    jobj("lower" to jint(it.lower), "upper" to (it.upper?.let(::jint) ?: "null"))
                })
            }),
            section("paceSeries", HarnessFixtures.paceCases.map { (name, input) ->
                name to renderPaceResult(PaceSeriesEngine.preprocess(input))
            }),
            section("heightFractions", HarnessFixtures.heightFractionCases.map { (name, c) ->
                name to jarr(heightFractions(c.first, c.second).map { jnum(it) })
            }),
            section("labelThinning", HarnessFixtures.labelThinningCases.map { (name, a) ->
                val count = a[0].toInt()
                val stride = labelStride(count, a[1], a[2], a[3])
                val visibility = if (count <= 0) "" else buildString {
                    for (i in 0 until count) append(if (isLabelVisible(i, count, stride)) '1' else '0')
                }
                name to jobj("stride" to jint(stride), "visibility" to jstr(visibility))
            }),
            section("barHitTest", HarnessFixtures.barHitTestCases.map { (name, a) ->
                name to (barIndexAtX(a[0], a[1], a[2], a[3].toInt())?.let(::jint) ?: "null")
            }),
            section("seriesSelection", buildList {
                listOf(
                    Triple("toggle_remove_last_kept", listOf(0), 0),
                    Triple("toggle_remove", listOf(0, 1), 1),
                    Triple("toggle_add", listOf(0), 2),
                    Triple("toggle_add_to_empty", emptyList(), 3),
                ).forEach { (name, cur, t) ->
                    add(name to jarr(SeriesSelection.toggled(cur, t).map { jint(it) }))
                }
                listOf(
                    Triple("norm_partial_kept", listOf(0, 5), setOf(1, 2, 3)),
                    Triple("norm_all_kept", listOf(2, 1), setOf(1, 2, 3)),
                    Triple("norm_empty_available", listOf(0), emptySet<Int>()),
                    Triple("norm_altitude_fallback", listOf(0), setOf(3)),
                ).forEach { (name, cur, avail) ->
                    add(name to jarr(SeriesSelection.normalized(cur, avail, PaceSeriesId.DISPLAY_PRIORITY).map { jint(it) }))
                }
                add("slots_pace_cad_only" to jarr(
                    SeriesSelection.assignSlots(PaceSeriesId.LINE_PRIORITY, setOf(0, 1, 2), setOf(0, 2)).map { jint(it) },
                ))
                add("slotAxis_0_to_3" to jarr((0..3).map { jstr(SeriesSelection.slotAxis(it).name) }))
            }),
        )
        return sections.joinToString(",\n", "{\n", "\n}\n")
    }

    // ── 렌더러(직렬화) ────────────────────────────────────────

    private fun section(name: String, cases: List<Pair<String, String>>): String =
        "  ${jstr(name)}: {\n" + cases.joinToString(",\n") { (k, v) -> "    ${jstr(k)}: $v" } + "\n  }"

    private fun renderPoints(points: List<Point>): String =
        digestList(points) { p -> jobj("x" to jnum(p.x), "y" to jnum(p.y)) }

    private fun renderNormPoints(points: List<NormalizedPoint>): String =
        digestList(points) { p -> jobj("x" to jnum(p.x), "y" to jnum(p.y)) }

    private fun renderNearest(n: NearestResult): String =
        jobj("seriesId" to jstr(n.seriesId), "x" to jnum(n.x), "y" to jnum(n.y))

    private fun renderLineLayout(l: LineChartLayout): String = jobj(
        "series" to jarr(l.series.map { s ->
            jobj(
                "id" to jstr(s.id),
                "role" to jstr(s.role.name),
                "pointCount" to jint(s.points.size),
                "points" to renderNormPoints(s.points),
            )
        }),
        "axisTicks" to jarr(l.axisTicks.map { at ->
            jobj(
                "axis" to jstr(at.axis.name),
                "tickCount" to jint(at.ticks.size),
                "ticks" to jarr(at.ticks.map { jobj("value" to jnum(it.value), "position" to jnum(it.position)) }),
            )
        }),
        "refBands" to jarr(l.refBands.map {
            jobj("axis" to jstr(it.axis.name), "lower" to jnum(it.lower), "upper" to jnum(it.upper))
        }),
        "markers" to jarr(l.markers.map {
            jobj("position" to jnum(it.position), "label" to jstr(it.label), "emphasis" to jbool(it.emphasis))
        }),
        "stats" to jobj(
            "perSeries" to jarr(l.stats.perSeries.map {
                jobj("id" to jstr(it.id), "min" to jnum(it.min), "max" to jnum(it.max), "avg" to jnum(it.avg))
            }),
            "segments" to jarr(l.stats.segments.map {
                jobj("min" to jnum(it.min), "max" to jnum(it.max), "avg" to jnum(it.avg), "count" to jint(it.count))
            }),
            "segmentSeriesId" to jstr(l.stats.segmentSeriesId),
        ),
        "domains" to jobj(
            "x" to renderDomain(l.domains.x),
            "yPrimary" to (l.domains.yPrimary?.let(::renderDomain) ?: "null"),
            "ySecondary" to (l.domains.ySecondary?.let(::renderDomain) ?: "null"),
        ),
    )

    private fun renderDomain(d: com.lumipol.graph.scale.AxisDomain): String =
        jobj("min" to jnum(d.min), "max" to jnum(d.max))

    private fun renderBarLayout(l: BarChartLayout): String = jobj(
        "barCount" to jint(l.bars.size),
        "bars" to jarr(l.bars.map {
            jobj(
                "index" to jint(it.index),
                "value" to jnum(it.value),
                "heightFraction" to jnum(it.heightFraction),
                "colorRole" to jstr(it.colorRole.name),
                "isPartial" to jbool(it.isPartial),
                "endMinutes" to (it.endMinutes?.let(::jint) ?: "null"),
            )
        }),
        "yTickCount" to jint(l.yTicks.size),
        "yTicks" to jarr(l.yTicks.map { jobj("value" to jnum(it.value), "position" to jnum(it.position)) }),
        "referenceLinePosition" to jnum(l.referenceLinePosition),
        "colorAnchors" to (l.colorAnchors?.let {
            jobj("fastest" to jnum(it.fastest), "slowest" to jnum(it.slowest), "average" to jnum(it.average))
        } ?: "null"),
    )

    private fun renderDonutLayout(l: DonutChartLayout): String = jobj(
        "segmentCount" to jint(l.segments.size),
        "segments" to jarr(l.segments.map {
            jobj(
                "startFraction" to jnum(it.startFraction),
                "sweepFraction" to jnum(it.sweepFraction),
                "value" to jnum(it.value),
                "colorRole" to jstr(it.colorRole.name),
                "sourceIndex" to jint(it.sourceIndex),
                "label" to jstr(it.label),
            )
        }),
        "total" to jnum(l.total),
    )

    private fun renderPaceResult(r: PaceSeriesResult): String = jobj(
        "pace" to renderPoints(r.pace),
        "heart" to renderPoints(r.heart),
        "cadence" to renderPoints(r.cadence),
        "altitudeArea" to (r.altitudeArea?.let { renderPoints(it) } ?: "null"),
        "bestPaceSeconds" to jnum(r.bestPaceSeconds),
        "validPaceCount" to jint(r.validPaceCount),
        // Set 순회 순서에 기대지 않는다 — 정렬해 결정론 확보
        "availableSeries" to jarr(r.availableSeries.sorted().map { jint(it) }),
    )
}
