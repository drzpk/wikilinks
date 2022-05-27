package dev.drzepka.wikilinks.app

import kotlin.system.exitProcess

actual fun exit(status: Int): Nothing {
    exitProcess(status)
}