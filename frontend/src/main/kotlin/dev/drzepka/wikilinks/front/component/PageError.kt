package dev.drzepka.wikilinks.front.component

import dev.drzepka.wikilinks.front.model.ErrorInfo
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.html.i
import io.kvision.html.span
import io.kvision.state.ObservableValue
import io.kvision.state.bind

class PageError(error: ObservableValue<ErrorInfo?>) : Div(className = "errors") {
    init {
        bind(error) {
            if (it != null) {
                div(className = "error") {
                    i(className = "bi bi-exclamation-octagon")
                    span(it.message)
                }
            }
        }
    }
}