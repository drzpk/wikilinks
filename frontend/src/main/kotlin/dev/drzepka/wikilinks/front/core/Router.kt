package dev.drzepka.wikilinks.front.core

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.front.model.RouteState
import dev.drzepka.wikilinks.front.model.SearchQuery
import dev.drzepka.wikilinks.front.model.State
import io.kvision.navigo.Match
import io.kvision.navigo.NavigateOptions
import io.kvision.routing.Routing
import org.w3c.dom.url.URLSearchParams

object Router : RouteState {
    private const val QUERY_SOURCE = "source"
    private const val QUERY_TARGET = "target"
    private const val QUERY_LANGUAGE = "language"

    private lateinit var state: State
    private lateinit var routing: Routing

    fun initialize(state: State) {
        this.state = state
        routing = Routing.init(root = "/", useHash = false)

        routing
            .on("/", this::rootRoute)
            .on("/:language", this::languageRoute)
            .resolve()
    }

    fun getLanguageRoute(language: DumpLanguage): DeferredRoute = DeferredRoute(routing, "/${language.value}")

    override fun putSearchQuery(query: SearchQuery) = doPutSearchQuery(query)

    private fun rootRoute(match: Match) {
        detectAndHandleSearchQuery(match.queryString, null)
    }

    private fun languageRoute(match: Match) {
        val language = DumpLanguage.fromString(match.data["language"] as String)
        if (!detectAndHandleSearchQuery(match.queryString, language) && language != null)
            state.selectLanguage(language)
    }

    private fun detectAndHandleSearchQuery(queryString: String, language: DumpLanguage?): Boolean {
        val params = URLSearchParams(queryString)
        val sourcePage = params.get(QUERY_SOURCE)
        val targetPage = params.get(QUERY_TARGET)
        val resolvedLanguage = language ?: params.get(QUERY_LANGUAGE)?.let { DumpLanguage.fromString(it) }

        return if (sourcePage != null && targetPage != null) {
            val query = SearchQuery(sourcePage, targetPage, resolvedLanguage)
            state.search(query)
            doPutSearchQuery(query, true)
            true
        } else false
    }

    private fun doPutSearchQuery(query: SearchQuery, replace: Boolean = false) {
        val options = object : NavigateOptions {}
        options.callHandler = false
        if (replace)
            options.historyAPIMethod = "replaceState"

        routing.navigate("/${query.language!!.value}?$QUERY_SOURCE=${query.sourcePage}&$QUERY_TARGET=${query.targetPage}", options)
    }
}

class DeferredRoute(private val routing: Routing, val url: String) {
    fun navigate() {
        routing.navigate(url)
    }
}
