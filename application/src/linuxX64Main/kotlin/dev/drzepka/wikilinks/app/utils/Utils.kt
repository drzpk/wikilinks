package dev.drzepka.wikilinks.app.utils

import kotlin.system.exitProcess

actual val environment: Environment = Environment.LINUX

actual fun exit(status: Int): Nothing {
    exitProcess(status)
}