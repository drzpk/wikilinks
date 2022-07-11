package dev.drzepka.wikilinks.app.utils

import java.nio.file.FileSystems
import kotlin.system.exitProcess

actual val environment: Environment = Environment.JVM

actual fun exit(status: Int): Nothing {
    exitProcess(status)
}


actual fun absolutePath(path: String): String =
    FileSystems.getDefault().getPath(path).normalize().toAbsolutePath().toString()
