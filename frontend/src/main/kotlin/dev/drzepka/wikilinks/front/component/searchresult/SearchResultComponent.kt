package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.state.ObservableState
import io.kvision.state.bind

class SearchResultComponent(result: ObservableState<LinkSearchResult?>) : Div(className = "search-result") {

    init {
        bind(result) {
            if (it != null) {
                add(ResultDescription(it))
                div("Found paths:", className = "found-paths-text")
                div(className = "paths") {
                    it.paths.forEach { path -> add(ResultPath(path, it.pages)) }
                }
            }
        }
    }
}
