package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.front.service.LanguageService
import dev.drzepka.wikilinks.front.service.LinkSearchService
import dev.drzepka.wikilinks.front.service.PageSearchService
import dev.drzepka.wikilinks.front.util.AnalyticsEvent
import dev.drzepka.wikilinks.front.util.DebounceBuffer
import dev.drzepka.wikilinks.front.util.ScopedAnalytics
import dev.drzepka.wikilinks.front.util.triggerAnalyticsEvent
import io.kvision.state.ObservableListWrapper
import io.kvision.state.ObservableValue
import kotlin.time.Duration.Companion.seconds

class State(
    pageSearchService: PageSearchService,
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
    val error = ObservableValue<ErrorInfo?>(null)

    val analytics = ScopedAnalytics<LinkSearchResult>()

    init {
        setupLanguages()

        sourceInput.selectedPage.subscribe { onSelectedPageChanged() }
        targetInput.selectedPage.subscribe { onSelectedPageChanged() }
        selectedLanguage.subscribe { onSelectedLanguageChanged() }

        historyState.getSearchQuery()?.let {
            initialSearch(it)
        }

        searchResult.subscribe {
            analytics.updateScope(it)
        }
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
        val language = query.language ?: DumpLanguage.EN
        selectedLanguage.setState(language)

        sourceInput.selectPage(null to query.sourcePage)
        targetInput.selectPage(null to query.targetPage)
        search(putHistory = false)
    }

    fun selectLanguage(language: DumpLanguage) {
        if (searchInProgress.value)
            return

        val previousLanguage = selectedLanguage.value
        selectedLanguage.setState(language)
        languageService.saveRecentLanguage(language)

        sourceInput.clear()
        targetInput.clear()
        searchResult.setState(null)
        historyState.clearSearchQuery()

        if (previousLanguage != null) {
            val event = AnalyticsEvent.LanguageChanged(previousLanguage, language)
            triggerAnalyticsEvent(event)
        }
    }

    fun search(putHistory: Boolean = true) {
        if (!canSearch.value || searchInProgress.value)
            return

        val source = sourceInput.selectedPage.value!!
        val target = targetInput.selectedPage.value!!

        if (putHistory)
            historyState.putSearchQuery(SearchQuery(source.second, target.second, selectedLanguage.value!!))

        searchInProgress.setState(true)
        error.setState(null)
        sourceInput.onSearch()
        targetInput.onSearch()
        updateCanSearch()

        val promise = if (source.first != null && target.first != null)
            linkSearchService.searchByIds(selectedLanguage.value!!, source.first!!, target.first!!)
        else
            linkSearchService.searchByTitles(selectedLanguage.value!!, source.second, target.second)

        promise
            .then {
                searchResult.setState(it)
            }
            .catch {
                handleException(it)
            }
            .then {
                searchInProgress.setState(false)
            }
    }

    private fun handleException(throwable: Throwable) {
        val info = if (throwable is ResponseException)
            ErrorInfo(throwable.response.message)
        else
            ErrorInfo("Unknown error occurred")

        searchResult.setState(null)
        error.setState(info)
    }

    private fun onSelectedPageChanged() = updateCanSearch()
    private fun onSelectedLanguageChanged() = updateCanSearch()

    private fun updateCanSearch() {
        canSearch.setState(
            selectedLanguage.value != null
                    && sourceInput.hasPage() && targetInput.hasPage()
                    && (sourceInput.selectedPageChanged() || targetInput.selectedPageChanged())
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
    val selectedPage = ObservableValue<Pair<Int?, String>?>(null)

    private val buffer = DebounceBuffer(400, ::searchForPage)
    private var previouslySearchedPage: Pair<Int?, String>? = null

    init {
        query.subscribe {
            if (selectedPage.value?.second != it) {
                selectedPage.setState(null)
            }

            buffer.execute(it)
        }
    }

    fun onFocusChanged(hasFocus: Boolean) {
        showHints.setState(hasFocus && hints.isNotEmpty())
    }

    fun selectHint(hint: PageHint) {
        setQuery(hint.title)

        selectedPage.setState(hint.id to hint.title)
        showHints.setState(false)
    }

    fun selectPage(page: Pair<Int?, String>) {
        setQuery(page.second)
        selectedPage.setState(page)
    }

    fun hasPage(): Boolean = selectedPage.value != null

    fun selectedPageChanged(): Boolean =
        selectedPage.value != null && selectedPage.value?.second != previouslySearchedPage?.second

    fun onSearch() {
        previouslySearchedPage = selectedPage.value
    }

    fun clear() {
        query.setState("")
        hints.clear()
        showHints.setState(false)
        selectedPage.setState(null)
    }

    private fun setQuery(value: String) {
        // Prevent hints from reappearing after manually setting the query
        buffer.disableFor(1.seconds)
        query.setState(value)
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
