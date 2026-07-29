package com.lumipol.graph.harness

import com.lumipol.graph.BarChartEngine
import com.lumipol.graph.ChartA11y
import com.lumipol.graph.ChartDefaults
import com.lumipol.graph.ChartFormat
import com.lumipol.graph.Gender
import com.lumipol.graph.TrackChartBuilder
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
            // B7 — 정책 상수 잠금: 값 변경은 골든 갱신(=의도 선언)을 강제한다.
            section("chartDefaults", buildList {
                add("LINE_WIDTH" to jnum(ChartDefaults.LINE_WIDTH))
                add("OVERLAY_LINE_WIDTH" to jnum(ChartDefaults.OVERLAY_LINE_WIDTH))
                add("GRADIENT_MAX_ALPHA" to jnum(ChartDefaults.GRADIENT_MAX_ALPHA))
                add("GRID_LINE_WIDTH" to jnum(ChartDefaults.GRID_LINE_WIDTH))
                add("GRID_DASH" to jarr(listOf(jnum(ChartDefaults.GRID_DASH_ON), jnum(ChartDefaults.GRID_DASH_OFF))))
                add("REF_DASH" to jarr(listOf(jnum(ChartDefaults.REF_DASH_ON), jnum(ChartDefaults.REF_DASH_OFF))))
                add("AREA_HEIGHT_FRACTION" to jnum(ChartDefaults.AREA_HEIGHT_FRACTION))
                add("AREA_MIN_VALUE_SPAN" to jnum(ChartDefaults.AREA_MIN_VALUE_SPAN))
                add("MARKER_LINE_WIDTH" to jnum(ChartDefaults.MARKER_LINE_WIDTH))
                add("MARKER_EMPHASIS_LINE_WIDTH" to jnum(ChartDefaults.MARKER_EMPHASIS_LINE_WIDTH))
                add("BAR_WIDTH_RATIO" to jnum(ChartDefaults.BAR_WIDTH_RATIO))
                add("PARTIAL_BAR_ALPHA" to jnum(ChartDefaults.PARTIAL_BAR_ALPHA))
                add("BAR_CORNER_RADIUS" to jnum(ChartDefaults.BAR_CORNER_RADIUS))
                add("BAR_MIN_HEIGHT" to jnum(ChartDefaults.BAR_MIN_HEIGHT))
                add("BAR_DIM_OPACITY" to jnum(ChartDefaults.BAR_DIM_OPACITY))
                add("BAR_CALLOUT_FONT_SIZE" to jnum(ChartDefaults.BAR_CALLOUT_FONT_SIZE))
                add("BAR_LABEL_GAP" to jnum(ChartDefaults.BAR_LABEL_GAP))
                add("BAR_X_LABEL_GAP" to jnum(ChartDefaults.BAR_X_LABEL_GAP))
                add("BAR_LABEL_MIN_GAP" to jnum(ChartDefaults.BAR_LABEL_MIN_GAP))
                add("CALLOUT_PAD_H" to jnum(ChartDefaults.CALLOUT_PAD_H))
                add("CALLOUT_PAD_V" to jnum(ChartDefaults.CALLOUT_PAD_V))
                add("CALLOUT_CORNER_RADIUS" to jnum(ChartDefaults.CALLOUT_CORNER_RADIUS))
                add("DONUT_RING_WIDTH" to jnum(ChartDefaults.DONUT_RING_WIDTH))
                add("DONUT_DIMMED_ALPHA" to jnum(ChartDefaults.DONUT_DIMMED_ALPHA))
                add("DONUT_CENTER_LABEL_FONT_SIZE" to jnum(ChartDefaults.DONUT_CENTER_LABEL_FONT_SIZE))
                add("DONUT_CENTER_PERCENT_FONT_SIZE" to jnum(ChartDefaults.DONUT_CENTER_PERCENT_FONT_SIZE))
                add("DONUT_AUTO_DESELECT_SECONDS" to jnum(ChartDefaults.DONUT_AUTO_DESELECT_SECONDS))
                add("DONUT_START_DEGREES" to jnum(ChartDefaults.DONUT_START_DEGREES))
                add("DONUT_CENTER_WIDTH_RATIO" to jnum(ChartDefaults.DONUT_CENTER_WIDTH_RATIO))
                add("MIN_HIT_TARGET_DP" to jnum(DonutEngine.MIN_HIT_TARGET_DP))
                add("AXIS_LABEL_FONT_SIZE" to jnum(ChartDefaults.AXIS_LABEL_FONT_SIZE))
                add("LABEL_GAP" to jnum(ChartDefaults.LABEL_GAP))
                add("AXIS_LABEL_GAP" to jnum(ChartDefaults.AXIS_LABEL_GAP))
                add("PLOT_INSETS" to jarr(listOf(
                    jnum(ChartDefaults.PLOT_INSET_TOP), jnum(ChartDefaults.PLOT_INSET_LEFT),
                    jnum(ChartDefaults.PLOT_INSET_BOTTOM), jnum(ChartDefaults.PLOT_INSET_RIGHT),
                )))
                add("TOUCH_LINE_WIDTH" to jnum(ChartDefaults.TOUCH_LINE_WIDTH))
                add("TOUCH_DOT_RADIUS" to jnum(ChartDefaults.TOUCH_DOT_RADIUS))
                add("MAX_ZOOM_SCALE" to jnum(ChartDefaults.MAX_ZOOM_SCALE))
                add("SCRUB_WINDOW_EPSILON" to jnum(com.lumipol.graph.query.SCRUB_WINDOW_EPSILON))
                add("ENTRANCE_EASING" to jarr(listOf(
                    jnum(ChartDefaults.ENTRANCE_EASING_X1), jnum(ChartDefaults.ENTRANCE_EASING_Y1),
                    jnum(ChartDefaults.ENTRANCE_EASING_X2), jnum(ChartDefaults.ENTRANCE_EASING_Y2),
                )))
                add("ENTRANCE_ENABLED_DEFAULT" to jbool(ChartDefaults.ENTRANCE_ENABLED_DEFAULT))
                add("ENTRANCE_DURATION_SECONDS" to jnum(ChartDefaults.ENTRANCE_DURATION_SECONDS))
                add("BAR_GROWTH_DURATION_SECONDS" to jnum(ChartDefaults.BAR_GROWTH_DURATION_SECONDS))
                add("DONUT_SWEEP_DURATION_SECONDS" to jnum(ChartDefaults.DONUT_SWEEP_DURATION_SECONDS))
                add("FALLBACK_DATA_COLOR" to jstr(hex(ChartDefaults.FALLBACK_DATA_COLOR)))
                val alphas = listOf(
                    "GRID_LINE_ALPHA" to ChartDefaults.GRID_LINE_ALPHA,
                    "OVERLAY_LINE_ALPHA" to ChartDefaults.OVERLAY_LINE_ALPHA,
                    "REF_BAND_ALPHA" to ChartDefaults.REF_BAND_ALPHA,
                    "AREA_FILL_ALPHA" to ChartDefaults.AREA_FILL_ALPHA,
                    "BAR_REFERENCE_LINE_ALPHA" to ChartDefaults.BAR_REFERENCE_LINE_ALPHA,
                    "BAR_SELECTION_LINE_ALPHA" to ChartDefaults.BAR_SELECTION_LINE_ALPHA,
                    "DONUT_ZONE2_ALPHA" to ChartDefaults.DONUT_ZONE2_ALPHA,
                    "DONUT_EMPTY_ALPHA" to ChartDefaults.DONUT_EMPTY_ALPHA,
                    "SECONDARY_LABEL_ALPHA" to ChartDefaults.SECONDARY_LABEL_ALPHA,
                )
                alphas.forEach { (k, v) -> add(k to jnum(v)) }
                val lp = ChartDefaults.LightPalette
                val dp = ChartDefaults.DarkPalette
                add("paletteLight" to jarr(listOf(
                    lp.PRIMARY_LINE, lp.SECONDARY_LINE, lp.GRID_LINE, lp.OVERLAY_LINE, lp.REF_BAND,
                    lp.AREA_FILL, lp.MARKER_LINE, lp.MARKER_EMPHASIS_LINE,
                    lp.BAR_REFERENCE_LINE, lp.BAR_SELECTION_LINE, lp.BAR_CALLOUT_BACKGROUND,
                    lp.BAR_CALLOUT_TEXT, lp.DONUT_ZONE1, lp.DONUT_ZONE2, lp.DONUT_ZONE3, lp.DONUT_ZONE4,
                    lp.DONUT_ZONE5, lp.DONUT_EMPTY, lp.DONUT_CENTER_LABEL, lp.DONUT_CENTER_PERCENT,
                    lp.AXIS_LABEL, lp.TOUCH_LINE,
                ).map { jstr(hex(it)) }))
                add("paletteDark" to jarr(listOf(
                    dp.PRIMARY_LINE, dp.SECONDARY_LINE, dp.GRID_LINE, dp.OVERLAY_LINE, dp.REF_BAND,
                    dp.AREA_FILL, dp.MARKER_LINE, dp.MARKER_EMPHASIS_LINE,
                    dp.BAR_REFERENCE_LINE, dp.BAR_SELECTION_LINE, dp.BAR_CALLOUT_BACKGROUND,
                    dp.BAR_CALLOUT_TEXT, dp.DONUT_ZONE1, dp.DONUT_ZONE2, dp.DONUT_ZONE3, dp.DONUT_ZONE4,
                    dp.DONUT_ZONE5, dp.DONUT_EMPTY, dp.DONUT_CENTER_LABEL, dp.DONUT_CENTER_PERCENT,
                    dp.AXIS_LABEL, dp.TOUCH_LINE,
                ).map { jstr(hex(it)) }))
            }),
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
            section("chartA11y", buildList {
                add("line_0_noArea" to jstr(ChartA11y.lineChart(0, false)))
                add("line_3_area" to jstr(ChartA11y.lineChart(3, true)))
                add("bar_0" to jstr(ChartA11y.barChart(0, emptyList())))
                add("bar_2_labeled" to jstr(ChartA11y.barChart(2, listOf("5'10\"", "5'30\""))))
                HarnessFixtures.donutCases.forEach { (name, data) ->
                    add("donut_$name" to jstr(ChartA11y.donut(DonutEngine.layout(data))))
                }
                add("donutSelection_labeled" to jstr(ChartA11y.donutSelection("저강도", 0.32)))
                add("donutSelection_unlabeled" to jstr(ChartA11y.donutSelection(null, 0.316)))
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
            section("trackChartBuilder", buildList {
                // C1 — 원천 전처리. 워치/GPS/폴백/게이트 각 1케이스 + 스플릿/HR존 dt 규칙.
                val totals = RunTotals(5000.0, 1500.0)
                val cumRows = listOf(
                    RawTrackSample(200.0, null, 60.0, null, null, 37.5665, 126.9780, 150.0, 170.0, 20.0),
                    RawTrackSample(200.0, null, 120.0, null, null, 37.5683, 126.9780, 155.0, null, 21.0), // 누적 정지 → 폴백
                    RawTrackSample(700.0, null, 180.0, null, null, 37.5701, 126.9780, 0.0, 172.0, 22.0),
                    RawTrackSample(705.0, null, 240.0, null, null, null, null, 158.0, 173.0, -150.0),     // 게이트 하한 무효
                )
                val kmDist = TrackChartBuilder.paceInput(cumRows, totals, BuildOptions(DistanceUnit.KILOMETERS, XMode.DISTANCE))
                add("cumulative_km_distance" to renderPaceInput(kmDist))
                add("cumulative_mile_time" to renderPaceInput(
                    TrackChartBuilder.paceInput(cumRows, totals, BuildOptions(DistanceUnit.MILES, XMode.TIME)),
                ))
                val deltaRows = listOf(
                    RawTrackSample(null, 200.0, 60.0, 60.0, 2.5, null, null, 150.0, 170.0, 20.0),
                    RawTrackSample(null, 210.0, 120.0, 60.0, 15.0, null, null, 152.0, 171.0, 20.5), // 워치 게이트 무효
                    RawTrackSample(null, 190.0, 180.0, 60.0, 3.0, null, null, 154.0, 172.0, 21.0),
                )
                add("watch_km_distance" to renderPaceInput(
                    TrackChartBuilder.paceInput(deltaRows, totals, BuildOptions(DistanceUnit.KILOMETERS, XMode.DISTANCE, useWatchSpeed = true)),
                ))
                add("splits_cumulative" to jarr(TrackChartBuilder.splitSamples(cumRows).map {
                    jobj("distanceMeters" to jnum(it.distanceMeters), "timeSeconds" to jnum(it.timeSeconds))
                }))
                add("zones_cumulative" to jarr(TrackChartBuilder.zoneSamples(cumRows).map {
                    jobj("heartRate" to jnum(it.heartRate), "timeInterval" to jnum(it.timeInterval))
                }))
                val legacyRows = listOf(
                    RawTrackSample(null, 10.0, null, 12.0, null, null, null, 150.0, null, null),
                    RawTrackSample(null, 10.0, null, -3.0, null, null, null, 155.0, null, null),
                )
                add("zones_legacy_fallback" to jarr(TrackChartBuilder.zoneSamples(legacyRows).map {
                    jobj("heartRate" to jnum(it.heartRate), "timeInterval" to jnum(it.timeInterval))
                }))
            }),
            section("chartFormat", buildList {
                listOf(270.0, 305.9, 5.9, 5939.9, 5940.0, 0.0).forEach {
                    add("pace_${jnum(it)}" to jstr(ChartFormat.pace(it)))
                }
                add("paceInvalid" to jstr(ChartFormat.paceInvalid()))
                listOf(0.0, 307.9, 3665.0).forEach { add("duration_${jnum(it)}" to jstr(ChartFormat.duration(it))) }
                listOf(0.316, 1.0).forEach { add("percent_${jnum(it)}" to jstr(ChartFormat.percent(it))) }
                listOf(5.0, 2.5, 0.5, 12.25, -1.5).forEach { add("distanceTick_${jnum(it)}" to jstr(ChartFormat.distanceTick(it))) }
                listOf(0.83844, 0.89, 1.0, 42.195).forEach {
                    add("splitEndDistance_${jnum(it)}" to jstr(ChartFormat.splitEndDistance(it)))
                }
                listOf(0.1, 15.0).forEach { add("timeTick_${jnum(it)}" to jstr(ChartFormat.timeTick(it))) }
                add("intTick_178.6" to jstr(ChartFormat.intTick(178.6)))
            }),
            section("heartRateHelpers", buildList {
                listOf(Gender.MALE, Gender.FEMALE, Gender.UNKNOWN).forEach { g ->
                    add("maxHR_30_${g.name}" to jint(HeartRateZoneEngine.maxHeartRate(30, g)))
                }
                add("segmentCount_10.5_distance" to jint(ChartConfig.segmentCountFor(10.5, XMode.DISTANCE)))
                add("segmentCount_10.5_time" to jint(ChartConfig.segmentCountFor(10.5, XMode.TIME)))
                add("segmentCount_500_distance" to jint(ChartConfig.segmentCountFor(500.0, XMode.DISTANCE)))
                (-1..2).forEach { slot ->
                    add("invertedAxes_slot_$slot" to jarr(
                        SeriesSelection.invertedAxesFor(slot).map { jstr(it.name) }.sorted(),
                    ))
                }
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
                @Suppress("DEPRECATION")
                add("slotAxis_0_to_3" to jarr((0..3).map { jstr(SeriesSelection.slotAxis(it).name) }))
                add("slotAxes_hr_cad" to jarr(
                    SeriesSelection.slotAxes(
                        listOf(PaceSeriesId.HEART, PaceSeriesId.CADENCE),
                        PaceSeriesId.SHARED_SCALE_IDS,
                    ).map { jstr(it.name) },
                ))
                add("slotAxes_pace_hr_cad" to jarr(
                    SeriesSelection.slotAxes(
                        listOf(PaceSeriesId.PACE, PaceSeriesId.HEART, PaceSeriesId.CADENCE),
                        PaceSeriesId.SHARED_SCALE_IDS,
                    ).map { jstr(it.name) },
                ))
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

    private fun renderPaceInput(input: PaceSeriesInput): String = jobj(
        "points" to jarr(input.points.map { p ->
            jobj(
                "x" to jnum(p.x),
                "paceSeconds" to jnum(p.paceSeconds),
                "heartRate" to (p.heartRate?.let(::jnum) ?: "null"),
                "cadence" to (p.cadence?.let(::jnum) ?: "null"),
                "altitude" to (p.altitude?.let(::jnum) ?: "null"),
            )
        }),
        "runningSeconds" to jnum(input.runningSeconds),
        "sumDistanceMeters" to jnum(input.sumDistanceMeters),
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
                "axis" to jstr(s.axis.name),
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
                "endDistanceMeters" to (it.endDistanceMeters?.let(::jnum) ?: "null"),
                "endSeconds" to (it.endSeconds?.let(::jnum) ?: "null"),
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
