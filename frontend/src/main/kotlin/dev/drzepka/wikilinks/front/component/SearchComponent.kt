package dev.drzepka.wikilinks.front.component

import dev.drzepka.wikilinks.front.model.State
import io.kvision.core.JustifyContent
import io.kvision.html.Button
import io.kvision.html.Div
import io.kvision.panel.hPanel
import io.kvision.state.bind

class SearchComponent(state: State) : Div(className = "search-component") {
    private val sourceInput = SearchInput(state.sourceInput)
    private val targetInput = SearchInput(state.targetInput)
    private val searchButton = Button("Search")

    init {
        searchButton.bind(state.searchButtonActive, runImmediately = true) {
            disabled = !it
        }

        hPanel(className = "inputs") {
            add(sourceInput)
            add(targetInput)
        }

        hPanel(justify = JustifyContent.CENTER) {
            add(searchButton)
        }
    }
}
