package dev.drzepka.wikilinks.common

import kotlinx.coroutines.CoroutineScope

actual fun <T> testRunBlocking(block: suspend CoroutineScope.() -> T): T {
    throw NotImplementedError("Not available on Linux")
}
