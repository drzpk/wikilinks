package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.front.service.PageSearchService
import dev.drzepka.wikilinks.front.util.DebounceBuffer
import io.kvision.state.ObservableListWrapper
import io.kvision.state.ObservableValue
import kotlin.time.Duration.Companion.seconds

class State {
    val sourceInput = SearchInputState()
    val targetInput = SearchInputState()
    val searchButtonActive = ObservableValue(false)

    init {
        sourceInput.selectedPage.subscribe { onSelectedPageChanged() }
        targetInput.selectedPage.subscribe { onSelectedPageChanged() }
    }

    private fun onSelectedPageChanged() {
        searchButtonActive.setState(sourceInput.selectedPage.value != null && targetInput.selectedPage.value != null)
    }
}

class SearchInputState {
    val query = ObservableValue("")
    val hints = ObservableListWrapper<PageHint>()
    val showHints = ObservableValue(false)
    val selectedPage = ObservableValue<Int?>(null)

    private val buffer = DebounceBuffer(500, ::searchForPage)
    private var ignoreQueryChange = false

    init {
        query.subscribe {
            if (ignoreQueryChange) {
                ignoreQueryChange = false
                return@subscribe
            }

            if (selectedPage.value != null)
                selectedPage.setState(null)

            buffer.execute(it)
        }
    }

    fun onFocusChanged(hasFocus: Boolean) {
        showHints.setState(hasFocus && hints.isNotEmpty())
    }

    fun selectHint(hint: PageHint) {
        // Prevent hints from reappearing after manually setting the query
        buffer.disableFor(1.seconds)

        query.setState(hint.title)
        selectedPage.setState(hint.id)
        showHints.setState(false)
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
