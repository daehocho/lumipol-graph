package com.lumipol.graph.harness

import com.lumipol.graph.BarChartEngine
import com.lumipol.graph.DonutEngine
import com.lumipol.graph.HeartRateZoneEngine
import com.lumipol.graph.LineChartEngine
import com.lumipol.graph.PaceColormap
import com.lumipol.graph.PaceSeriesEngine
import com.lumipol.graph.PaceSeriesId
import com.lumipol.graph.SeriesSelection
import com.lumipol.graph.interaction.ZoomWindow
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

    @Suppress("DEPRECATION") // nearest 섹션은 deprecated 경로의 골든 관측을 유지한다(제거 시점까지)
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
            section("nearestScrub", buildList {
                val l09 = HarnessFixtures.lineCases.first { it.first == "L09_pace_hr_markers" }.second
                val fullLayout = LineChartEngine.layout(l09)
                HarnessFixtures.nearestXs.forEach { x ->
                    add("x_${jnum(x)}" to renderScrub(LineChartEngine.nearestScrub(l09, fullLayout, x)))
                }
                val windowLayout = LineChartEngine.layout(l09, 1.0, 2.0)
                add("windowed_1.0_2.0_x_2.4" to renderScrub(LineChartEngine.nearestScrub(l09, windowLayout, 2.4)))
            }),
            section("interpolatedY", HarnessFixtures.interpolationCases.flatMap { (name, points, xs) ->
                xs.map { x -> "${name}_x_${jnum(x)}" to jnum(LineChartEngine.interpolatedY(points, x)) }
            }),
            section("zoomWindow", buildList {
                val full = ZoomWindow(0.0, 10.0)
                add("initial" to renderZoom(full))
                val pinched = full.pinch(full.windowMin, full.windowMax, 2.0, 0.5, 10.0)
                add("pinch_2.0_anchor_0.5" to renderZoom(pinched))
                add("pinch_cumulative_4.0_anchor_0.25" to renderZoom(
                    pinched.pinch(full.windowMin, full.windowMax, 4.0, 0.25, 10.0),
                ))
                add("pinch_clamped_max_10" to renderZoom(full.pinch(full.windowMin, full.windowMax, 100.0, 0.5, 10.0)))
                add("pan_0.2_from_pinched" to renderZoom(pinched.pan(pinched.windowMin, pinched.windowMax, 0.2)))
                add("setWindow_8_13" to renderZoom(full.setWindow(8.0, 13.0)))
                // ulp 재구성 회귀 — 완전 줌아웃 시 fullDomain 비트 복원(isZoomed=false)
                val ulp = ZoomWindow(21.730886, 195.28034191195613)
                    .pinch(21.730886, 195.28034191195613, 4.0, 0.7, 10.0)
                add("ulp_full_zoom_out" to renderZoom(ulp.pinch(ulp.windowMin, ulp.windowMax, 0.1, 0.3, 10.0)))
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
            section("donutHitTest", buildList {
                // 비율 공간 프로브 — 조각 경계에서 떨어진 지점만(각도 libm ULP와 무관한 이산 결과).
                val probes = listOf(
                    Triple(0.01, -1.0, 0.2), Triple(1.0, 0.01, 0.2), Triple(-1.0, 0.0, 0.2),
                    Triple(0.7, -0.7, 0.2), Triple(0.0, 0.0, 0.2), Triple(0.0, -1.5, 0.2),
                    Triple(0.0, -0.65, 0.8),
                )
                HarnessFixtures.donutCases.forEach { (name, data) ->
                    val layout = DonutEngine.layout(data)
                    probes.forEachIndexed { i, (dx, dy, band) ->
                        add("${name}_p$i" to (DonutEngine.hitTest(dx, dy, band, layout)?.let(::jint) ?: "null"))
                    }
                }
            }),
            section("paceColormap", buildList {
                // f=300/s=400/a=350 — pace1=315, pace2=362.5. 값 그리드는 경계 정확점 포함(이산 규칙 관측).
                val anchors = BarColorAnchors(300.0, 400.0, 350.0)
                val values = listOf(250.0, 300.0, 303.0, 310.0, 315.0, 338.75, 350.0, 362.5, 390.0, 400.0, 450.0)
                values.forEach { v ->
                    add("rgba_${jnum(v)}" to jstr(hex(PaceColormap.rgba(v, anchors, colorBlind = false))))
                    add("rgba_cb_${jnum(v)}" to jstr(hex(PaceColormap.rgba(v, anchors, colorBlind = true))))
                }
                add("legendStops_40" to jarr(PaceColormap.legendStops(anchors).map { jstr(hex(it)) }))
                add("legendStops_cb_8" to jarr(
                    PaceColormap.legendStops(anchors, count = 8, colorBlind = true).map { jstr(hex(it)) },
                ))
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

    /** 0xAARRGGBB → "#AARRGGBB" — 색은 문자열로 기록해 T1(완전 일치) 비교를 강제한다. */
    private fun hex(argb: Long): String {
        val h = argb.toULong().toString(16).uppercase().padStart(8, '0')
        return "#$h"
    }

    private fun renderZoom(z: ZoomWindow): String = jobj(
        "windowMin" to jnum(z.windowMin),
        "windowMax" to jnum(z.windowMax),
        "isZoomed" to jbool(z.isZoomed),
        "scale" to jnum(z.scale),
    )

    private fun renderNearest(n: NearestResult): String =
        jobj("seriesId" to jstr(n.seriesId), "x" to jnum(n.x), "y" to jnum(n.y))

    private fun renderScrub(r: ScrubResult?): String = r?.let {
        jobj(
            "snappedX" to jnum(it.snappedX),
            "snappedNx" to jnum(it.snappedNx),
            "snapSourceId" to jstr(it.snapSourceId),
            "perSeries" to jarr(it.perSeries.map { p ->
                jobj(
                    "seriesId" to jstr(p.seriesId),
                    "x" to jnum(p.x),
                    "y" to jnum(p.y),
                    "nx" to jnum(p.nx),
                    "ny" to (p.ny?.let(::jnum) ?: "null"),
                    "role" to jstr(p.role.name),
                    "axis" to jstr(p.axis.name),
                    "chartAxis" to jstr(p.chartAxis.name),
                )
            }),
        )
    } ?: "null"

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
