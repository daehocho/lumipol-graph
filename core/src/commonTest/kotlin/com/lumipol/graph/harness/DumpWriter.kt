package com.lumipol.graph.harness

/**
 * 덤프 파일 기록 — 테스트 전용 expect/actual (프로덕션 소스에는 여전히 플랫폼 분기 없음).
 * 기본 출력 디렉토리는 /tmp/lumipol-graph-dump — JVM·iOS 시뮬레이터 프로세스 모두
 * 호스트 파일시스템의 같은 경로에 쓸 수 있어 diff가 간단해진다.
 */
expect val dumpPlatformName: String

/** [content]를 기록하고 절대 경로를 반환한다. */
expect fun writeDumpFile(fileName: String, content: String): String
