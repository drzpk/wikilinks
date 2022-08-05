package dev.drzepka.wikilinks.front

import dev.drzepka.wikilinks.front.component.SearchComponent
import dev.drzepka.wikilinks.front.component.header.HeaderComponent
import dev.drzepka.wikilinks.front.component.searchresult.SearchResultComponent
import dev.drzepka.wikilinks.front.model.HistoryState
import dev.drzepka.wikilinks.front.model.SearchQuery
import dev.drzepka.wikilinks.front.model.State
import dev.drzepka.wikilinks.front.service.MockLinkSearchService
import dev.drzepka.wikilinks.front.service.MockPageSearchService
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
        io.kvision.require("./css/app.css")
        io.kvision.require("./css/loader.css")
        io.kvision.require("flag-icons/css/flag-icons.min.css")
    }

    private lateinit var state: State

    override fun start(state: Map<String, Any>) {
        console.log(state)
        this.state = (state["state"] as State?) ?: createState()

        root("wikilinks", ContainerType.FLUID) {
            add(HeaderComponent(this@Frontend.state))

            responsiveGridPanel {
                options(1, 1, 6, 3) {
                    add(SearchComponent(this@Frontend.state))
                }

                options(2, 1, 12, 0) {
                    add(SearchResultComponent(this@Frontend.state.searchResult))
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

        return if (source != null && target != null)
            SearchQuery(source, target)
        else null
    }

    override fun putSearchQuery(query: SearchQuery) {
        val params = URLSearchParams(window.location.search)
        params.set(QUERY_SOURCE, query.sourcePage)
        params.set(QUERY_TARGET, query.targetPage)

        val url = "${window.location.pathname}?$params"
        window.history.pushState(null, "", url)
    }

    private fun createState(): State {
        val pageSearchService = if (USE_MOCKS) MockPageSearchService else PageSearchServiceImpl()
        val linkSearchService = if (USE_MOCKS) MockLinkSearchService else LinkSearchServiceImpl()

        return State(pageSearchService, linkSearchService, this)
    }

    companion object {
        private const val USE_MOCKS = false
        private const val QUERY_SOURCE = "source"
        private const val QUERY_TARGET = "target"
    }
}
