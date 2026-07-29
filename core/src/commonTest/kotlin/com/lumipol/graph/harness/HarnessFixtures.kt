package com.lumipol.graph.harness

import com.lumipol.graph.model.*

/**
 * 0.7단계 고정 입력 데이터셋 (docs/refactor/07-harness.md §입력 데이터셋).
 *
 * 전부 결정론적이다 — 난수·시각은 물론, sin/log 같은 초월 함수도 쓰지 않는다.
 * 초월 함수는 JVM/Native에서 ULP가 어긋날 수 있어, 픽스처 생성 단계에서 쓰면
 * "코어 로직 차이"와 "픽스처 생성 차이"가 diff에서 구분되지 않는다.
 * 파형이 필요한 곳은 정수 모듈러 연산으로 만든다(정수→Double 변환은 정확).
 */
object HarnessFixtures {

    // ── 라인 차트 ─────────────────────────────────────────────
    // L09/L10/L12는 iOS 스냅샷 픽스처(ios-renderer TestFixtures.swift)의 미러 —
    // "실제 앱 대표 케이스" 역할(x=km 0.0~5.0, 0.5 간격 11점).
    private val paceValues = listOf(6.1, 5.9, 5.75, 5.6, 5.7, 5.5, 5.35, 5.45, 5.3, 5.2, 5.4)
    private val heartValues = listOf(148.0, 152.0, 157.0, 160.0, 163.0, 166.0, 168.0, 170.0, 172.0, 174.0, 171.0)

    private fun gridSeries(
        id: String,
        values: List<Double>,
        axis: Axis = Axis.PRIMARY,
        role: SeriesRole = SeriesRole.MAIN,
    ) = Series(id, values.mapIndexed { i, y -> Point(i * 0.5, y) }, axis, role)

    private val kmMarkers = (1..5).map { Marker(it.toDouble(), "${it}km", emphasis = it == 5) }

    val lineCases: List<Pair<String, LineChartData>> = listOf(
        "L01_empty" to LineChartData(emptyList()),
        "L02_single_point" to LineChartData(listOf(Series("s", listOf(Point(2.0, 5.0))))),
        "L03_two_points_same_x" to LineChartData(
            listOf(Series("s", listOf(Point(1.0, 5.0), Point(1.0, 7.0)))),
            config = ChartConfig(segmentCount = 3), // span=0 스플릿 분기 관측
        ),
        "L04_two_points_same_y" to LineChartData(listOf(Series("s", listOf(Point(0.0, 5.0), Point(1.0, 5.0))))),
        "L05_constant_series" to LineChartData(listOf(gridSeries("s", List(5) { 42.0 }))),
        "L06a_nan_inf_y" to LineChartData(
            listOf(gridSeries("s", listOf(5.0, Double.NaN, 6.0, Double.POSITIVE_INFINITY, 7.0, Double.NEGATIVE_INFINITY))),
        ),
        "L06b_nan_x" to LineChartData(
            listOf(Series("s", listOf(Point(0.0, 5.0), Point(Double.NaN, 6.0), Point(2.0, 7.0)))),
        ),
        "L07_negative_zero_cross" to LineChartData(listOf(gridSeries("s", listOf(-50.0, -20.0, 0.0, 25.0, 50.0)))),
        "L08_extreme_mixed" to LineChartData(
            listOf(Series("s", listOf(Point(0.0, 1.0e-9), Point(1.0, 5.0e11), Point(2.0, 1.0e12), Point(3.0, 2.5e-7)))),
        ),
        "L09_pace_hr_markers" to LineChartData(
            series = listOf(gridSeries("pace", paceValues), gridSeries("hr", heartValues, Axis.SECONDARY)),
            segmentMarkers = kmMarkers,
            config = ChartConfig(segmentCount = 5, maxTicks = 5),
        ),
        "L10_full_refband" to LineChartData(
            series = listOf(gridSeries("pace", paceValues), gridSeries("hr", heartValues, Axis.SECONDARY)),
            referenceBands = listOf(RefBand(5.4, 5.6, Axis.PRIMARY)),
            segmentMarkers = kmMarkers,
            config = ChartConfig(segmentCount = 5, maxTicks = 5),
        ),
        "L11_overlay_self_normalized" to LineChartData(
            series = listOf(
                gridSeries("pace", paceValues),
                gridSeries("cad_overlay", paceValues.map { 170.0 + it * 2 }, Axis.PRIMARY, SeriesRole.OVERLAY),
            ),
        ),
        "L12_pace_hr_cad_shared_secondary" to LineChartData(
            series = listOf(
                gridSeries("pace", paceValues),
                gridSeries("hr", heartValues, Axis.SECONDARY),
                gridSeries("cad", paceValues.map { 170.0 + it * 2 }, Axis.SECONDARY),
            ),
            segmentMarkers = kmMarkers,
            config = ChartConfig(segmentCount = 5, maxTicks = 5),
        ),
    )

    /** (라벨, 대상 픽스처, xMin, xMax) — viewport layout 관측. */
    val windowedCases: List<Pair<String, Triple<LineChartData, Double, Double>>> = run {
        val l09 = lineCases.first { it.first == "L09_pace_hr_markers" }.second
        listOf(
            "W01_L09_mid" to Triple(l09, 1.25, 3.75),
            "W02_L09_gap_between_points" to Triple(l09, 2.1, 2.4),
            "W03_L09_outside_right" to Triple(l09, 10.0, 20.0),
            "W04_L09_partial_left" to Triple(l09, -1.0, 0.75),
        )
    }

    /** 배경 area 전용 layout — (라벨, 데이터, area). */
    val backgroundOnlyCases: List<Pair<String, Pair<LineChartData, List<Point>?>>> = listOf(
        "BG01_area_only" to (LineChartData(emptyList()) to listOf(Point(0.0, 10.0), Point(2.0, 14.0), Point(4.0, 12.0), Point(6.0, 20.0))),
        "BG02_single_point_area_fallback" to (LineChartData(emptyList()) to listOf(Point(1.0, 5.0))),
        "BG03_zero_width_area_fallback" to (LineChartData(emptyList()) to listOf(Point(2.0, 5.0), Point(2.0, 8.0))),
        "BG04_series_present_fallback" to (LineChartData(listOf(Series("s", listOf(Point(0.0, 1.0), Point(1.0, 2.0))))) to listOf(Point(0.0, 10.0), Point(6.0, 20.0))),
        "BG05_null_area" to (LineChartData(emptyList()) to null),
    )

    // ── 막대 차트 ─────────────────────────────────────────────
    private fun bars(vararg blocks: Pair<Int, SplitSample>): List<SplitSample> =
        blocks.flatMap { (n, s) -> List(n) { s } }

    val barCases: List<Pair<String, BarChartData>> = listOf(
        "B01_empty" to BarChartData(emptyList(), splitDistanceMeters = 1000.0),
        "B02_invalid_only" to BarChartData(
            listOf(
                SplitSample(0.0, 10.0), SplitSample(100.0, 0.0), SplitSample(-5.0, 20.0),
                SplitSample(Double.NaN, 30.0), SplitSample(100.0, Double.NaN),
                SplitSample(Double.POSITIVE_INFINITY, 10.0), SplitSample(100.0, Double.POSITIVE_INFINITY),
                SplitSample(50.0, -3.0),
            ),
            splitDistanceMeters = 1000.0,
        ),
        // 3km: 빠름(280s/km)-정상(300)-느림(330), 경계 정확히 떨어짐 → 부분 스플릿 없음
        "B03_three_exact_splits" to BarChartData(
            bars(10 to SplitSample(100.0, 28.0), 10 to SplitSample(100.0, 30.0), 10 to SplitSample(100.0, 33.0)),
            splitDistanceMeters = 1000.0,
        ),
        "B04_partial_tail" to BarChartData(
            bars(10 to SplitSample(100.0, 28.0), 10 to SplitSample(100.0, 30.0), 10 to SplitSample(100.0, 33.0), 5 to SplitSample(100.0, 31.0)),
            splitDistanceMeters = 1000.0,
        ),
        // 300m 샘플이 1km 경계를 가로지름 → 오버플로 시간 비례 배분 관측
        "B05_boundary_overflow" to BarChartData(
            bars(8 to SplitSample(300.0, 90.0)),
            splitDistanceMeters = 1000.0,
        ),
        "B06_explicit_target" to BarChartData(
            bars(10 to SplitSample(100.0, 28.0), 10 to SplitSample(100.0, 30.0), 10 to SplitSample(100.0, 33.0)),
            splitDistanceMeters = 1000.0,
            targetPaceSecPerUnit = 300.0,
            toleranceSecPerUnit = 5.0,
        ),
        // 시간모드: 총 1230s/6150m, 버킷 300s → 4 full + 1 partial, endMinutes 반올림 경계(20.5분) 포함
        "B07_time_mode_with_totals" to BarChartData(
            bars(123 to SplitSample(50.0, 10.0)),
            splitDistanceMeters = 1000.0,
            splitTimeSeconds = 300.0,
            totalDurationSeconds = 1230.0,
            totalDistanceMeters = 6150.0,
        ),
        "B08_time_mode_no_totals" to BarChartData(
            bars(123 to SplitSample(50.0, 10.0)),
            splitDistanceMeters = 1000.0,
            splitTimeSeconds = 300.0,
        ),
        // 0.64km(10m/7.5s = 750s/km) — 100m 버킷 하향 관측(온전 6 + 부분 1)
        "B09_short_run_subdivided" to BarChartData(
            bars(64 to SplitSample(10.0, 7.5)),
            splitDistanceMeters = 1000.0,
        ),
        // 0.2km — 하한(50m)으로도 5막대 미달(4막대)
        "B10_below_min_bars_floor" to BarChartData(
            bars(20 to SplitSample(10.0, 7.5)),
            splitDistanceMeters = 1000.0,
        ),
        // 3분/30초 버킷 — endMinutes 중복(1,1,2,2,3,3) 대비 endSeconds 관측
        "B11_sub_minute_time_bucket" to BarChartData(
            bars(180 to SplitSample(3.3333333333333335, 1.0)),
            splitDistanceMeters = 1000.0,
            splitTimeSeconds = 30.0,
            totalDurationSeconds = 180.0,
            totalDistanceMeters = 600.0,
        ),
        // 거리모드 + 총거리(690m) > 유효 합(640m) — 마지막 부분 스플릿 끝 스냅 관측(0.42.0)
        "B12_distance_mode_total_snap" to BarChartData(
            bars(64 to SplitSample(10.0, 7.5)),
            splitDistanceMeters = 1000.0,
            totalDurationSeconds = 480.0,
            totalDistanceMeters = 690.0,
        ),
    )

    /** chooseTimeBucketSeconds 입력(초) — 버킷 후보 경계 전후 + 하향(30/15초) 경계. */
    val timeBucketCases: List<Double> = listOf(
        0.0, 59.0, 60.0, 120.0, 150.0, 180.0, 240.0, 299.0, 300.0,
        600.0, 601.0, 1200.0, 1201.0, 3000.0, 3001.0, 6000.0, 6001.0, 100000.0,
    )

    // ── 도넛 ─────────────────────────────────────────────────
    private val zones = DonutColorRole.entries

    val donutCases: List<Pair<String, DonutChartData>> = listOf(
        "N01_empty" to DonutChartData(emptyList()),
        "N02_all_nonpositive" to DonutChartData(
            listOf(DonutSegment(0.0, zones[0]), DonutSegment(-5.0, zones[1]), DonutSegment(0.0, zones[2])),
        ),
        "N03_five_zones" to DonutChartData(
            listOf(45.0, 210.0, 640.5, 300.25, 120.0).mapIndexed { i, v -> DonutSegment(v, zones[i], "존${i + 1}") },
        ),
        // value<=0·NaN 필터 → sourceIndex가 원본 인덱스(1,3)를 유지하는지 관측
        "N04_filtered_source_index" to DonutChartData(
            listOf(
                DonutSegment(0.0, zones[0]),
                DonutSegment(10.0, zones[1], "유산소"),
                DonutSegment(-3.0, zones[2]),
                DonutSegment(30.0, zones[3], "무산소"),
                DonutSegment(0.0, zones[4]),
                DonutSegment(Double.NaN, zones[0]),
            ),
        ),
        "N05_single" to DonutChartData(listOf(DonutSegment(42.0, zones[4]))),
    )

    /** (current, tapped) → 토글 전이표. */
    val donutToggleCases: List<Pair<Int?, Int?>> = listOf(
        null to null, null to 2, 2 to 2, 2 to 3, 2 to null,
    )

    // ── 심박존 ────────────────────────────────────────────────
    // maxHR=190 기준 존 경계(95/114/133/152/171bpm)의 정확 경계·직전 값 + 무효 샘플.
    val hrZoneSamples: List<HeartRateZoneSample> = listOf(
        HeartRateZoneSample(95.0, 10.0), HeartRateZoneSample(94.9, 10.0),
        HeartRateZoneSample(114.0, 10.0), HeartRateZoneSample(113.9, 10.0),
        HeartRateZoneSample(133.0, 10.0), HeartRateZoneSample(152.0, 10.0),
        HeartRateZoneSample(171.0, 10.0), HeartRateZoneSample(189.0, 10.0),
        HeartRateZoneSample(250.0, 10.0),
        HeartRateZoneSample(0.0, 10.0), HeartRateZoneSample(-5.0, 10.0),
        HeartRateZoneSample(160.0, 0.0), HeartRateZoneSample(160.0, -2.0),
    )
    val hrZoneMaxHrCases: List<Int> = listOf(0, 185, 190, 200)

    // ── 페이스 전처리 ─────────────────────────────────────────
    private fun samplePoint(
        i: Int,
        pace: Double,
        hr: Double? = 150.0,
        cad: Double? = 170.0,
        alt: Double? = 30.0,
        dxKm: Double = 0.01,
    ) = PaceSamplePoint(x = i * dxKm, paceSeconds = pace, heartRate = hr, cadence = cad, altitude = alt)

    /**
     * 대표 케이스 — 10km 러닝, 600표본. 파형은 전부 정수 모듈러(초월 함수 금지 — 파일 상단 주석).
     * 앞쪽 40표본 심박 결측(첫 유효값 소급), 케이던스·고도 주기 결측(직전 승계),
     * 상한 초과(1500)·하한 미달(100)·p95 컷 후보(900) 스파이크 포함.
     * avg = 3300/(10000/1000) = 330 → filterMax = 930.
     */
    private fun representativeRun(): PaceSeriesInput {
        val n = 600
        val points = (0 until n).map { i ->
            val basePace = 330.0 + ((i * 37) % 80).toDouble() - 40.0 // 290~369
            val pace = when {
                i % 101 == 50 -> 1500.0 // filterMax(930) 초과 → 0 처리 기대
                i % 97 == 60 -> 900.0   // 필터 통과, p95×1.25 컷 후보
                i % 89 == 30 -> 100.0   // 하한(120) 미달 → 0 처리 기대
                else -> basePace
            }
            PaceSamplePoint(
                x = i * (10.0 / n),
                paceSeconds = pace,
                heartRate = if (i < 40) null else 140.0 + ((i * 13) % 35).toDouble(),
                cadence = if (i % 7 == 3) null else 160.0 + ((i * 11) % 20).toDouble(),
                altitude = if (i % 13 == 5) null else 20.0 + (if (i % 120 < 60) i % 120 else 120 - i % 120).toDouble() * 0.5,
            )
        }
        return PaceSeriesInput(points, runningSeconds = 3300.0, sumDistanceMeters = 10000.0)
    }

    private fun uniformRun(n: Int): PaceSeriesInput = PaceSeriesInput(
        points = (0 until n).map { i -> samplePoint(i, 300.0 + (i % 10), hr = 150.0 + (i % 20), dxKm = 12.0 / n) },
        runningSeconds = 3600.0,
        sumDistanceMeters = 12000.0,
    )

    val paceCases: List<Pair<String, PaceSeriesInput>> = listOf(
        "P01_zero_totals" to PaceSeriesInput(listOf(samplePoint(0, 300.0)), runningSeconds = 0.0, sumDistanceMeters = 0.0),
        "P02_empty_points" to PaceSeriesInput(emptyList(), runningSeconds = 100.0, sumDistanceMeters = 1000.0),
        "P03_all_optional_missing" to PaceSeriesInput(
            (0 until 15).map { i -> samplePoint(i, 300.0, hr = null, cad = null, alt = null) },
            runningSeconds = 300.0, sumDistanceMeters = 1000.0,
        ),
        // 유효 표본 10개 = MIN_VALID_PACE_COUNT(11) 직전 → hasPace=false 기대
        "P04_valid_10_below_min" to PaceSeriesInput(
            (0 until 10).map { i -> samplePoint(i, 300.0 + i) },
            runningSeconds = 300.0, sumDistanceMeters = 1000.0,
        ),
        "P05_valid_11_at_min" to PaceSeriesInput(
            (0 until 11).map { i -> samplePoint(i, 300.0 + i) },
            runningSeconds = 300.0, sumDistanceMeters = 1000.0,
        ),
        // p95 아웃라이어 컷 발동 경계: 19표본(미발동) vs 20표본(발동)
        "P06a_outlier_19_no_cap" to PaceSeriesInput(
            (0 until 19).map { i -> samplePoint(i, if (i == 18) 800.0 else 300.0 + i) },
            runningSeconds = 3000.0, sumDistanceMeters = 10000.0,
        ),
        "P06b_outlier_20_cap" to PaceSeriesInput(
            (0 until 20).map { i -> samplePoint(i, if (i == 19) 800.0 else 300.0 + i) },
            runningSeconds = 3000.0, sumDistanceMeters = 10000.0,
        ),
        "P07_nan_pace" to PaceSeriesInput(
            (0 until 30).map { i -> samplePoint(i, if (i == 15) Double.NaN else 300.0 + (i % 5)) },
            runningSeconds = 3000.0, sumDistanceMeters = 10000.0,
        ),
        // 다운샘플 skip 경계: 5999(skip=1) vs 6000(skip=2)
        "P08a_downsample_5999" to uniformRun(5999),
        "P08b_downsample_6000" to uniformRun(6000),
        "P09_representative_10km" to representativeRun(),
    )

    // ── 쿼리 함수 직접 ─────────────────────────────────────────
    /** niceScale (min, max, maxTicks, headroomFraction). */
    val niceScaleCases: List<Pair<String, DoubleArray>> = listOf(
        "zero_zero" to doubleArrayOf(0.0, 0.0, 5.0, 0.0),
        "basic_0_100" to doubleArrayOf(0.0, 100.0, 5.0, 0.0),
        "degenerate_5_5" to doubleArrayOf(5.0, 5.0, 5.0, 0.0),
        "neg3_7" to doubleArrayOf(-3.0, 7.0, 5.0, 0.0),
        "tiny_range" to doubleArrayOf(0.001, 0.0011, 5.0, 0.0),
        "huge_0_1e12" to doubleArrayOf(0.0, 1.0e12, 5.0, 0.0),
        "maxticks_2" to doubleArrayOf(2.5, 97.5, 2.0, 0.0),
        "maxticks_10" to doubleArrayOf(2.5, 97.5, 10.0, 0.0),
        "headroom_0_100" to doubleArrayOf(0.0, 100.0, 5.0, 0.05),
        "headroom_neg50_50" to doubleArrayOf(-50.0, 50.0, 5.0, 0.05),
        "headroom_zeromin_clamp" to doubleArrayOf(0.0, 10.0, 5.0, 0.05),
        "reversed_10_0" to doubleArrayOf(10.0, 0.0, 5.0, 0.0),
    )

    val heightFractionCases: List<Pair<String, Pair<List<Double>, Double>>> = listOf(
        "hf_empty" to (emptyList<Double>() to 0.0),
        "hf_single" to (listOf(5.0) to 0.0),
        "hf_constant" to (listOf(3.0, 3.0, 3.0) to 0.0),
        "hf_normal" to (listOf(0.0, 5.0, 10.0) to 0.0),
        "hf_minspan_flatten" to (listOf(10.0, 10.2) to 0.5),
        "hf_minspan_noop" to (listOf(0.0, 5.0, 10.0) to 2.0),
    )

    /** labelStride/isLabelVisible (count, plotWidthPx, labelWidthPx, gapPx). */
    val labelThinningCases: List<Pair<String, DoubleArray>> = listOf(
        "lt_zero_count" to doubleArrayOf(0.0, 320.0, 40.0, 4.0),
        "lt_43_splits" to doubleArrayOf(43.0, 320.0, 40.0, 4.0),
        "lt_44_last_multiple_hidden" to doubleArrayOf(44.0, 320.0, 40.0, 4.0),
        "lt_10_no_thinning" to doubleArrayOf(10.0, 320.0, 20.0, 0.0),
        "lt_zero_width" to doubleArrayOf(5.0, 0.0, 40.0, 0.0),
        "lt_zero_label" to doubleArrayOf(5.0, 320.0, 0.0, 0.0),
        "lt_negative_gap" to doubleArrayOf(5.0, 320.0, 40.0, -5.0),
    )

    /** barIndexAtX (x, plotMinX, plotWidth, count). */
    val barHitTestCases: List<Pair<String, DoubleArray>> = listOf(
        "bh_below_clamp" to doubleArrayOf(-10.0, 0.0, 300.0, 10.0),
        "bh_zero" to doubleArrayOf(0.0, 0.0, 300.0, 10.0),
        "bh_just_before_boundary" to doubleArrayOf(29.999, 0.0, 300.0, 10.0),
        "bh_on_boundary" to doubleArrayOf(30.0, 0.0, 300.0, 10.0),
        "bh_last_slot" to doubleArrayOf(299.9, 0.0, 300.0, 10.0),
        "bh_above_clamp" to doubleArrayOf(400.0, 0.0, 300.0, 10.0),
        "bh_zero_count" to doubleArrayOf(150.0, 0.0, 300.0, 0.0),
        "bh_zero_width" to doubleArrayOf(150.0, 0.0, 0.0, 10.0),
        "bh_offset_plot" to doubleArrayOf(45.0, 10.0, 290.0, 43.0),
    )

    /** interpolatedY — (라벨, points, 질의 x 목록). */
    val interpolationCases: List<Triple<String, List<Point>, List<Double>>> = listOf(
        Triple("ip_empty", emptyList(), listOf(1.0)),
        Triple("ip_single", listOf(Point(2.0, 5.0)), listOf(0.0, 2.0, 9.0)),
        Triple(
            "ip_multi_dup_x",
            listOf(Point(0.0, 0.0), Point(1.0, 10.0), Point(1.0, 20.0), Point(3.0, 30.0)),
            listOf(-1.0, 0.0, 0.5, 1.0, 2.0, 3.0, 4.0),
        ),
    )

    /** nearest — L09 기준 질의 x. 2.25는 2.0/2.5 정중앙(동률 규칙 관측). */
    val nearestXs: List<Double> = listOf(2.4, 2.25, -5.0, 100.0)
}
