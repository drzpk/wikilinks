package dev.drzepka.wikilinks.app.utils

import kotlin.system.exitProcess

actual val environment: Environment = Environment.JVM

actual fun exit(status: Int): Nothing {
    exitProcess(status)
}