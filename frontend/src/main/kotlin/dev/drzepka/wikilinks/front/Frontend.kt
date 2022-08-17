package dev.drzepka.wikilinks.front

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.front.component.SearchComponent
import dev.drzepka.wikilinks.front.component.header.HeaderComponent
import dev.drzepka.wikilinks.front.component.searchresult.SearchResultComponent
import dev.drzepka.wikilinks.front.model.HistoryState
import dev.drzepka.wikilinks.front.model.SearchQuery
import dev.drzepka.wikilinks.front.model.State
import dev.drzepka.wikilinks.front.service.MockLanguageService
import dev.drzepka.wikilinks.front.service.MockLinkSearchService
import dev.drzepka.wikilinks.front.service.MockPageSearchService
import dev.drzepka.wikilinks.front.service.impl.LanguageServiceImpl
import dev.drzepka.wikilinks.front.service.impl.LinkSearchServiceImpl
import dev.drzepka.wikilinks.front.service.impl.PageSearchServiceImpl
import io.kvision.Application
import io.kvision.panel.ContainerType
import io.kvision.panel.responsiveGridPanel
import io.kvision.panel.root
import kotlinx.browser.window
import org.w3c.dom.url.URLSearchParams

class Frontend : Application(), HistoryState {
    init {
        io.kvision.require("./css/root.scss")
        io.kvision.require("bootstrap-icons/font/bootstrap-icons.css")
        io.kvision.require("flag-icons/css/flag-icons.min.css")
    }

    private lateinit var state: State

    override fun start(state: Map<String, Any>) {
        console.log(state)
        this.state = (state["state"] as State?) ?: createState()

        root("wikilinks", ContainerType.FLUID) {
            add(HeaderComponent(this@Frontend.state))

            responsiveGridPanel {
                options(1, 1, 12, 0) {
                    add(SearchComponent(this@Frontend.state))
                }

                options(1, 2, 12, 0) {
                    add(SearchResultComponent(this@Frontend.state.searchResult, this@Frontend.state.analytics))
                }
            }
        }
    }

    override fun dispose(): Map<String, Any> {
        // todo: this doesn't seem to be working
        return mapOf(
            "state" to state
        )
    }

    override fun getSearchQuery(): SearchQuery? {
        val params = URLSearchParams(window.location.search)
        val source = params.get(QUERY_SOURCE)
        val target = params.get(QUERY_TARGET)
        val language = params.get(QUERY_LANGUAGE)?.let { DumpLanguage.fromString(it) }

        return if (source != null && target != null)
            SearchQuery(source, target, language)
        else null
    }

    override fun putSearchQuery(query: SearchQuery) {
        val params = URLSearchParams(window.location.search)
        params.set(QUERY_SOURCE, query.sourcePage)
        params.set(QUERY_TARGET, query.targetPage)
        query.language?.apply { params.set(QUERY_LANGUAGE, value) }

        val url = "${window.location.pathname}?$params"
        window.history.pushState(null, "", url)
    }

    override fun clearSearchQuery() {
        val params = URLSearchParams(window.location.search)
        params.delete(QUERY_SOURCE)
        params.delete(QUERY_TARGET)
        params.delete(QUERY_LANGUAGE)

        val query = "?$params".let { if (it == "?") "" else it }
        val url = "${window.location.pathname}$query"
        window.history.pushState(null, "", url)
    }

    private fun createState(): State {
        val pageSearchService = if (USE_MOCKS) MockPageSearchService else PageSearchServiceImpl()
        val linkSearchService = if (USE_MOCKS) MockLinkSearchService else LinkSearchServiceImpl()
        val languageService = if (USE_MOCKS) MockLanguageService else LanguageServiceImpl()

        return State(pageSearchService, linkSearchService, languageService, this)
    }

    companion object {
        private const val USE_MOCKS = false
        private const val QUERY_SOURCE = "source"
        private const val QUERY_TARGET = "target"
        private const val QUERY_LANGUAGE = "language"
    }
}
