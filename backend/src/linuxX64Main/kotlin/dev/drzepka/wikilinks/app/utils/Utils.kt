package dev.drzepka.wikilinks.app.utils

import kotlinx.cinterop.*
import platform.posix._PC_PATH_MAX
import platform.posix.pathconf
import platform.posix.realpath
import kotlin.system.exitProcess

actual val environment: Environment = Environment.LINUX

actual fun exit(status: Int): Nothing {
    exitProcess(status)
}

actual fun absolutePath(path: String): String = memScoped {
    val buffer = nativeHeap.allocArray<ByteVar>(4096)
    realpath(path, buffer)
    buffer.toKString()
}