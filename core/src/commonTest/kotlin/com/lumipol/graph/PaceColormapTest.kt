package com.lumipol.graph

import com.lumipol.graph.model.BarColorAnchors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B6 — 페이스 컬러맵 단일 원본. 기대값은 렌더러 float 공식의 8비트 반올림 양자화. */
class PaceColormapTest {

    // f=300, s=400, a=350 → pace1 = 350-50*0.7 = 315, pace2 = 350+50*0.25 = 362.5
    private val anchors = BarColorAnchors(fastest = 300.0, slowest = 400.0, average = 350.0)

    @Test
    fun degenerate_range_falls_back_to_green() {
        val flat = BarColorAnchors(300.0, 300.0, 300.0)
        assertEquals(0xFF00FF00, PaceColormap.rgba(300.0, flat, colorBlind = false))
        assertEquals(PaceColormap.COLOR_BLIND_GREEN, PaceColormap.rgba(300.0, flat, colorBlind = true))
    }

    @Test
    fun continuous_zone_endpoints() {
        // 가장 빠름(f): 1구간 cv=1 → rgb(0, 1-0.4, 1) = (0, 153, 255)
        assertEquals(0xFF0099FF, PaceColormap.rgba(300.0, anchors, colorBlind = false))
        // pace1 정확히 = 2구간 시작 cv=1 → rgb(0,255,0)
        assertEquals(0xFF00FF00, PaceColormap.rgba(315.0, anchors, colorBlind = false))
        // pace2 = 3구간 시작 cv=1 → rgb(255,255,0)
        assertEquals(0xFFFFFF00, PaceColormap.rgba(362.5, anchors, colorBlind = false))
        // 가장 느림(s): cv=0 → rgb(255,0,0)
        assertEquals(0xFFFF0000, PaceColormap.rgba(400.0, anchors, colorBlind = false))
    }

    @Test
    fun continuous_out_of_range_clamps() {
        // f보다 빠른 값 → f와 동일 색, s보다 느린 값 → s와 동일 색
        assertEquals(PaceColormap.rgba(300.0, anchors, false), PaceColormap.rgba(250.0, anchors, false))
        assertEquals(PaceColormap.rgba(400.0, anchors, false), PaceColormap.rgba(450.0, anchors, false))
    }

    @Test
    fun color_blind_band_rule_follows_ios_split() {
        // len1=15 → 파랑 문턱 303, len2=47.5 → 노랑 문턱 338.75
        assertEquals(PaceColormap.COLOR_BLIND_BLUE, PaceColormap.rgba(301.0, anchors, colorBlind = true))
        assertEquals(PaceColormap.COLOR_BLIND_GREEN, PaceColormap.rgba(310.0, anchors, colorBlind = true))
        assertEquals(PaceColormap.COLOR_BLIND_GREEN, PaceColormap.rgba(330.0, anchors, colorBlind = true))
        assertEquals(PaceColormap.COLOR_BLIND_YELLOW, PaceColormap.rgba(350.0, anchors, colorBlind = true))
        assertEquals(PaceColormap.COLOR_BLIND_RED, PaceColormap.rgba(390.0, anchors, colorBlind = true))
        // 범위 밖 클램프
        assertEquals(PaceColormap.COLOR_BLIND_BLUE, PaceColormap.rgba(0.0, anchors, colorBlind = true))
        assertEquals(PaceColormap.COLOR_BLIND_RED, PaceColormap.rgba(999.0, anchors, colorBlind = true))
    }

    @Test
    fun legend_stops_default_count_and_endpoints() {
        val stops = PaceColormap.legendStops(anchors)
        assertEquals(PaceColormap.LEGEND_STOP_COUNT, stops.size)
        assertEquals(PaceColormap.rgba(300.0, anchors, false), stops.first())
        assertEquals(PaceColormap.rgba(400.0, anchors, false), stops.last())
    }

    @Test
    fun legend_stops_color_blind_are_discrete_bands() {
        val stops = PaceColormap.legendStops(anchors, count = 40, colorBlind = true)
        val distinct = stops.distinct()
        assertEquals(
            listOf(
                PaceColormap.COLOR_BLIND_BLUE,
                PaceColormap.COLOR_BLIND_GREEN,
                PaceColormap.COLOR_BLIND_YELLOW,
                PaceColormap.COLOR_BLIND_RED,
            ),
            distinct,
        )
    }

    @Test
    fun legend_stops_degenerate_and_tiny_counts() {
        assertEquals(1, PaceColormap.legendStops(anchors, count = 1).size)
        assertEquals(1, PaceColormap.legendStops(anchors, count = 0).size)
        val flat = BarColorAnchors(300.0, 300.0, 300.0)
        val stops = PaceColormap.legendStops(flat, count = 5)
        assertEquals(5, stops.size)
        assertTrue(stops.all { it == 0xFF00FF00 })
    }
}
