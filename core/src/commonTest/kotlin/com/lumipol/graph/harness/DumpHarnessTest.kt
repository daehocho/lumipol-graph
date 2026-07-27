package com.lumipol.graph.harness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 0.7단계 덤프 진입점 — 실행하면 /tmp/lumipol-graph-dump/core-dump-<플랫폼>.json 이 생성된다.
 * 실행 커맨드·diff 절차: docs/refactor/07-harness.md (실행은 사용자가 한다).
 */
class DumpHarnessTest {

    @Test
    fun writeCoreDump() {
        val json = CoreDump.build()
        assertTrue(json.startsWith("{"), "dump must be a JSON object")
        val path = writeDumpFile("core-dump-$dumpPlatformName.json", json)
        println("[lumipol-harness] core dump written: $path (${json.length} chars)")
    }

    /** 같은 프로세스에서 두 번 조립해도 동일해야 한다 — 플랫폼 내 결정론 스모크 체크. */
    @Test
    fun dumpIsDeterministicWithinPlatform() {
        assertEquals(CoreDump.build(), CoreDump.build())
    }
}
