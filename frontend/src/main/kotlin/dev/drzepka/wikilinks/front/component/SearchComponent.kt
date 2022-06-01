package dev.drzepka.wikilinks.front.component

import io.kvision.core.JustifyContent
import io.kvision.core.onEvent
import io.kvision.form.text.TextInput
import io.kvision.html.Button
import io.kvision.html.Div
import io.kvision.panel.hPanel

class SearchComponent : Div(className = "search-component") {
    private val sourceTextInput: TextInput
    private val targetTextInput: TextInput
    private val searchButton: Button

    init {
        sourceTextInput = TextInput {
            onEvent {
                input = { onSearchInputChanged(0) }
            }
        }

        targetTextInput = TextInput {
            onEvent {
                input = { onSearchInputChanged(1) }
            }
        }

        searchButton = Button("Search")
        searchButton.disabled = true

        hPanel(className = "inputs") {
            add(sourceTextInput)
            add(targetTextInput)
        }

        hPanel(justify = JustifyContent.CENTER) {
            add(searchButton)
        }
    }

    private fun onSearchInputChanged(position: Int) {

    }
}
