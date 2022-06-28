package dev.drzepka.wikilinks.common

import kotlinx.coroutines.CoroutineScope

expect fun<T> testRunBlocking(block: suspend CoroutineScope.() -> T): T
