package dev.drzepka.wikilinks.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun <T> testRunBlocking(block: suspend CoroutineScope.() -> T): T = runBlocking(block = block)
