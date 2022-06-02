package dev.drzepka.wikilinks.front.util

import kotlinx.browser.window
import kotlin.js.Date
import kotlin.time.Duration

class DebounceBuffer<T>(private val delayMs: Int, private val handler: (value: T) -> Unit) {
    private var marker = 0
    private var disabledUntil = 0.0

    fun execute(value: T) {
        if (disabledUntil > Date().getTime())
            return

        marker++

        val currentMarker = marker
        window.setTimeout({
            tryToHandle(value, currentMarker)
        }, delayMs)
    }

    fun disableFor(duration: Duration) {
        disabledUntil = Date().getTime() + duration.inWholeMilliseconds
    }

    private fun tryToHandle(value: T, oldMarker: Int) {
        if (oldMarker== marker)
            handler.invoke(value)
    }
}
