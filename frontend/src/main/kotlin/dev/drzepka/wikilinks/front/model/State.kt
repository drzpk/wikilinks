package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.front.service.PageSearchService
import dev.drzepka.wikilinks.front.util.DebounceBuffer
import io.kvision.state.ObservableListWrapper
import io.kvision.state.ObservableValue

class State {
    val sourceInput = SearchInputState()
    val targetInput = SearchInputState()

    val searchButtonActive = ObservableValue(false)
}

class SearchInputState {
    val query = ObservableValue("")
    val hints = ObservableListWrapper<PageHint>()
    val showHints = ObservableValue(false)

    private val buffer = DebounceBuffer(500, ::searchForPage)

    init {
        query.subscribe { buffer.execute(it) }
    }

    fun onFocusChanged(hasFocus: Boolean) {
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
