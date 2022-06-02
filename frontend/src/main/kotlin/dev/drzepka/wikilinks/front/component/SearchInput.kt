package dev.drzepka.wikilinks.front.component

import dev.drzepka.wikilinks.front.model.SearchInputState
import io.kvision.core.onEvent
import io.kvision.form.text.textInput
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.state.bindEach
import io.kvision.state.bindTo

class SearchInput(private val state: SearchInputState) : Div() {

    init {
        textInput {
            bindTo(state.query)

            onEvent {
                focus = { state.onFocusChanged(true) }
                blur = { state.onFocusChanged(false) }
            }
        }

        div(className = "search-hints-container") {
            div(className = "search-hints").bindEach(this@SearchInput.state.hints) { hint -> add(SearchHint(hint)) }

            this@SearchInput.state.showHints.subscribe {
                visible = it
            }
        }
    }
}
