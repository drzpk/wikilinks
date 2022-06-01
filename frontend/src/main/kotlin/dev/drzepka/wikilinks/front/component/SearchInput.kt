package dev.drzepka.wikilinks.front.component

import dev.drzepka.wikilinks.front.model.PageHint
import dev.drzepka.wikilinks.front.service.PageSearchService
import dev.drzepka.wikilinks.front.util.DebounceBuffer
import io.kvision.core.onEvent
import io.kvision.form.text.TextInput
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.state.ObservableListWrapper
import io.kvision.state.ObservableValue
import io.kvision.state.bindEach

class SearchInput : Div() {
    private val input: TextInput
    private val buffer = DebounceBuffer(500, ::searchForPage)
    private val hints = ObservableListWrapper<PageHint>()

    private val showHints = ObservableValue(false)

    init {
        input = TextInput {
            onEvent {
                input = { buffer.execute(self.value ?: "") }
                focus = { onInputFocusChanged(true) }
                blur = { onInputFocusChanged(false) }
            }
        }

        add(input)

        div(className = "search-hints-container") {
            div(className = "search-hints").bindEach(this@SearchInput.hints) { hint -> add(SearchHint(hint)) }

            this@SearchInput.showHints.subscribe {
                visible = it
            }
        }
    }

    private fun onInputFocusChanged(hasFocus: Boolean) {
        showHints.setState(hasFocus && hints.isNotEmpty())
    }

    private fun searchForPage(query: String) {
        if (query.isBlank()) {
            showHints.setState(false)
            return
        } else {
            showHints.setState(true)
        }

        val results = PageSearchService.query(query)
        hints.clear()
        hints.addAll(results)
    }
}
