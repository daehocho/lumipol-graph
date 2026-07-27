package com.lumipol.graph.harness

import kotlin.test.Test
import kotlin.test.assertEquals

/** 고정 자릿수 포매터 자체 검증 — 기대 문자열은 플랫폼 무관해야 한다. */
class JsonDumpTest {

    @Test
    fun formatsFixedTwelveDigits() {
        assertEquals("0.000000000000", jnum(0.0))
        assertEquals("0.000000000000", jnum(-0.0)) // -0 정규화
        assertEquals("0.500000000000", jnum(0.5))
        assertEquals("-2.500000000000", jnum(-2.5))
        assertEquals("0.100000000000", jnum(0.1))
        assertEquals("0.333333333333", jnum(1.0 / 3.0))
        assertEquals("42.000000000000", jnum(42.0))
        assertEquals("1000000000000.000000000000", jnum(1.0e12))
        assertEquals("0.000000001000", jnum(1.0e-9))
        assertEquals("0.000000000000", jnum(1.0e-13)) // 12자리 밖 → 반올림 0
        assertEquals("2.675000000000", jnum(2.675))   // 13자리째 반올림 캐리
        assertEquals("5.000000000000", jnum(4.9999999999999))
    }

    @Test
    fun formatsSpecialValues() {
        assertEquals("\"NaN\"", jnum(Double.NaN))
        assertEquals("\"Infinity\"", jnum(Double.POSITIVE_INFINITY))
        assertEquals("\"-Infinity\"", jnum(Double.NEGATIVE_INFINITY))
        assertEquals("null", jnum(null as Double?))
    }

    @Test
    fun escapesStrings() {
        assertEquals("\"a\\\"b\\\\c\\nd\"", jstr("a\"b\\c\nd"))
        assertEquals("\"존5\"", jstr("존5"))
        assertEquals("null", jstr(null))
    }

    @Test
    fun digestSamplesDeterministically() {
        val big = (0 until 100).toList()
        val digest = digestList(big) { it.toString() }
        // step = ceil(100/32) = 4 → 0,4,...,96 (25개) + 마지막 99
        assertEquals(true, digest.contains("\"count\":100"))
        assertEquals(true, digest.contains("\"sampleStep\":4"))
        assertEquals(true, digest.endsWith("]}"))
        val small = digestList(listOf(1, 2, 3)) { it.toString() }
        assertEquals("{\"count\":3,\"items\":[1,2,3]}", small)
    }
}
