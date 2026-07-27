package com.lumipol.graph.harness

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile

actual val dumpPlatformName: String = "iosSimulatorArm64"

@OptIn(ExperimentalForeignApi::class)
actual fun writeDumpFile(fileName: String, content: String): String {
    // 시뮬레이터 테스트 프로세스는 호스트 파일시스템을 그대로 본다 — /tmp 직접 기록.
    val dirPath = "/tmp/lumipol-graph-dump"
    NSFileManager.defaultManager.createDirectoryAtPath(dirPath, true, null, null)
    val path = "$dirPath/$fileName"
    @Suppress("CAST_NEVER_SUCCEEDS")
    (content as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    return path
}
