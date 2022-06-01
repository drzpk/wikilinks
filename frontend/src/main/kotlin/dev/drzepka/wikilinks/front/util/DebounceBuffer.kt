package dev.drzepka.wikilinks.front.util

import kotlinx.browser.window

class DebounceBuffer<T>(private val delayMs: Int, private val handler: (value: T) -> Unit) {
    private var marker = 0

    fun execute(value: T) {
        marker++

        val currentMarker = marker
        window.setTimeout({
            tryToHandle(value, currentMarker)
        }, delayMs)
    }

    private fun tryToHandle(value: T, oldMarker: Int) {
        if (oldMarker== marker)
            handler.invoke(value)
    }
}
