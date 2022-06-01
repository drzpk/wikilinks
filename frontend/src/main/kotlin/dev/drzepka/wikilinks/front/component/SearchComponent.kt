package dev.drzepka.wikilinks.front.component

import io.kvision.core.JustifyContent
import io.kvision.html.Button
import io.kvision.html.Div
import io.kvision.panel.hPanel

class SearchComponent : Div(className = "search-component") {
    private val sourceInput = SearchInput()
    private val targetInput = SearchInput()
    private val searchButton = Button("Search")

    init {
        searchButton.disabled = true

        hPanel(className = "inputs") {
            add(sourceInput)
            add(targetInput)
        }

        hPanel(justify = JustifyContent.CENTER) {
            add(searchButton)
        }
    }
}
