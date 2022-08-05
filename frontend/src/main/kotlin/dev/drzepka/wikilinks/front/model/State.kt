package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.front.service.LanguageService
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
    private val languageService: LanguageService,
    private val historyState: HistoryState
) {
    val availableLanguages = ObservableListWrapper(mutableListOf<DumpLanguage>())
    val selectedLanguage = ObservableValue<DumpLanguage?>(null)

    val sourceInput = SearchInputState(selectedLanguage, pageSearchService)
    val targetInput = SearchInputState(selectedLanguage, pageSearchService)

    val canSearch = ObservableValue(false)
    val searchInProgress = ObservableValue(false)
    val searchResult = ObservableValue<LinkSearchResult?>(null)

    init {
        setupLanguages()

        sourceInput.selectedPage.subscribe { onSelectedPageChanged() }
        targetInput.selectedPage.subscribe { onSelectedPageChanged() }
        selectedLanguage.subscribe { onSelectedLanguageChanged() }

        historyState.getSearchQuery()?.let { initialSearch(it) }
    }

    private fun setupLanguages() {
        languageService.getAvailableLanguages().then {
            val languages = it.map { info -> info.language }
            availableLanguages.addAll(languages)

            if (selectedLanguage.value == null) {
                val recent = languageService.getRecentLanguage() ?: DumpLanguage.EN
                val selected = if (recent in languages) recent else languages.firstOrNull()
                selectedLanguage.setState(selected)
            }
        }
    }

    private fun initialSearch(query: SearchQuery) {
        searchInProgress.setState(true)

        val language = query.language ?: DumpLanguage.EN
        selectedLanguage.setState(language)

        var source: PageHint? = null
        var target: PageHint? = null

        pageSearchService.search(language, query.sourcePage, true).then {
            source = it.firstOrNull()

            if (source != null)
                pageSearchService.search(language, query.targetPage, true)
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

    fun selectLanguage(language: DumpLanguage) {
        if (searchInProgress.value)
            return

        selectedLanguage.setState(language)
        languageService.saveRecentLanguage(language)

        sourceInput.clear()
        targetInput.clear()
        searchResult.setState(null)
        historyState.clearSearchQuery()
    }

    fun search() {
        if (!canSearch.value || searchInProgress.value)
            return

        val source = sourceInput.selectedPage.value!!
        val target = targetInput.selectedPage.value!!
        historyState.putSearchQuery(SearchQuery(source.second, target.second, selectedLanguage.value!!))

        searchInProgress.setState(true)
        linkSearchService.search(selectedLanguage.value!!, source.first, target.first)
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

    private fun onSelectedPageChanged() = updateCanSearch()
    private fun onSelectedLanguageChanged() = updateCanSearch()

    private fun updateCanSearch() {
        canSearch.setState(
            selectedLanguage.value != null
                    && sourceInput.selectedPage.value != null
                    && targetInput.selectedPage.value != null
        )
    }
}

class SearchInputState(
    private val selectedLanguage: ObservableValue<DumpLanguage?>,
    private val pageSearchService: PageSearchService
) {
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

    fun clear() {
        query.setState("")
        hints.clear()
        showHints.setState(false)
        selectedPage.setState(null)
    }

    private fun searchForPage(query: String) {
        if (query.isBlank()) {
            showHints.setState(false)
            return
        } else if (selectedLanguage.value == null) {
            return
        }

        pageSearchService.search(selectedLanguage.value!!, query)
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
    fun clearSearchQuery()
}
