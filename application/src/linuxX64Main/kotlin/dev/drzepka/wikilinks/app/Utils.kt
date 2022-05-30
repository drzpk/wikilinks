package dev.drzepka.wikilinks.app

import kotlin.system.exitProcess

actual val environment: Environment = Environment.LINUX

actual fun exit(status: Int): Nothing {
    exitProcess(status)
}