package dev.drzepka.wikilinks.front.component

import dev.drzepka.wikilinks.front.model.State
import io.kvision.core.Display
import io.kvision.core.JustifyContent
import io.kvision.html.*
import io.kvision.panel.hPanel
import io.kvision.state.bind

class SearchComponent(state: State) : Div(className = "search-component") {
    private val sourceInput = SearchInput(state.sourceInput, state.searchInProgress)
    private val targetInput = SearchInput(state.targetInput, state.searchInProgress)
    private val searchButton = Button("Search")

    init {
        searchButton.onClick { state.search() }
        searchButton.bind(state.canSearch, runImmediately = true) {
            disabled = !it
        }
        searchButton.bind(state.searchInProgress) {
            display = if (it) Display.NONE else Display.INITIAL
        }

        h2("Find the shortest paths between Wikipedia articles.") {
            id = "headline"
        }

        div(className = "inputs") {
            add(this@SearchComponent.sourceInput)
            i(className = "bi bi-arrow-right")
            add(this@SearchComponent.targetInput)
        }

        hPanel(justify = JustifyContent.CENTER) {
            add(searchButton)
        }

        div(className = "loader") {
            div(className = "lds-facebook") {
                div()
                div()
                div()
            }

            bind(state.searchInProgress, removeChildren = false) {
                display = if (it) Display.BLOCK else Display.NONE
            }
        }
    }
}
