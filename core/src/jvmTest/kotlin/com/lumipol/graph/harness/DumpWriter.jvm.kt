package com.lumipol.graph.harness

import java.io.File

actual val dumpPlatformName: String = "jvm"

actual fun writeDumpFile(fileName: String, content: String): String {
    val dir = File(System.getenv("LUMIPOL_DUMP_DIR") ?: "/tmp/lumipol-graph-dump")
    dir.mkdirs()
    val f = File(dir, fileName)
    f.writeText(content)
    return f.absolutePath
}
