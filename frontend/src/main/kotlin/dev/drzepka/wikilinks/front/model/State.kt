package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.front.service.LinkSearchService
import dev.drzepka.wikilinks.front.service.PageSearchService
import dev.drzepka.wikilinks.front.util.DebounceBuffer
import io.kvision.state.ObservableListWrapper
import io.kvision.state.ObservableValue
import kotlin.js.Promise
import kotlin.time.Duration.Companion.seconds

class State(
    private val pageSearchService: PageSearchService,
    private val linkSearchService: LinkSearchService,
    private val historyState: HistoryState
) {
    val sourceInput = SearchInputState(pageSearchService)
    val targetInput = SearchInputState(pageSearchService)

    val canSearch = ObservableValue(false)
    val searchInProgress = ObservableValue(false)
    val searchResult = ObservableValue<LinkSearchResult?>(null)

    init {
        sourceInput.selectedPage.subscribe { onSelectedPageChanged() }
        targetInput.selectedPage.subscribe { onSelectedPageChanged() }

        historyState.getSearchQuery()?.let { initialSearch(it) }
    }

    private fun initialSearch(query: SearchQuery) {
        searchInProgress.setState(true)
        var source: PageHint? = null
        var target: PageHint? = null

        pageSearchService.search(query.sourcePage, true).then {
            source = it.firstOrNull()

            if (source != null)
                pageSearchService.search(query.targetPage, true)
            else Promise.resolve(emptyList())
        }.then {
            target = it.firstOrNull()
        }.catch {
            it.printStackTrace()
        }.then {
            searchInProgress.setState(false)
            if (source != null && target != null) {
                sourceInput.selectHint(source!!)
                targetInput.selectHint(target!!)
                search()
            }
        }
    }

    fun search() {
        if (!canSearch.value || searchInProgress.value)
            return

        val source = sourceInput.selectedPage.value!!
        val target = targetInput.selectedPage.value!!
        historyState.putSearchQuery(SearchQuery(source.second, target.second))

        searchInProgress.setState(true)
        linkSearchService.search(source.first, target.first)
            .then {
                searchResult.setState(it)
            }
            .catch {
                console.error("Error while downloading links", it)
            }
            .then {
                searchInProgress.setState(false)
            }
    }

    private fun onSelectedPageChanged() {
        canSearch.setState(sourceInput.selectedPage.value != null && targetInput.selectedPage.value != null)
    }
}

class SearchInputState(private val pageSearchService: PageSearchService) {
    val query = ObservableValue("")
    val hints = ObservableListWrapper<PageHint>()
    val showHints = ObservableValue(false)
    val selectedPage = ObservableValue<Pair<Int, String>?>(null)

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
        selectedPage.setState(hint.id to hint.title)
        showHints.setState(false)
    }

    private fun searchForPage(query: String) {
        if (query.isBlank()) {
            showHints.setState(false)
            return
        }

        pageSearchService.search(query)
            .then {
                showHints.setState(true)
                hints.clear()
                hints.addAll(it)
            }
            .catch {
                console.error("Error while searching for page with query '$query'", it)
            }
    }
}

interface HistoryState {
    fun getSearchQuery(): SearchQuery?
    fun putSearchQuery(query: SearchQuery)
}
