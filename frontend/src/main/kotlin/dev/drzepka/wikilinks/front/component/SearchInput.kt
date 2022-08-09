package dev.drzepka.wikilinks.front.component

import dev.drzepka.wikilinks.front.model.SearchInputState
import io.kvision.core.onEvent
import io.kvision.form.text.textInput
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.state.ObservableState
import io.kvision.state.bind
import io.kvision.state.bindEach
import io.kvision.state.bindTo

class SearchInput(private val state: SearchInputState, private val searchInProgress: ObservableState<Boolean>) : Div() {
    private var inputHasFocus = false
    private var mouseOverHints = false
    private var previousFocusValue = false

    init {
        val input = textInput {
            bindTo(state.query)
            bind(searchInProgress) { disabled = it }

            onEvent {
                focus = {
                    inputHasFocus = true
                    updateStateFocus()
                }
                blur = {
                    inputHasFocus = false
                    updateStateFocus()
                }
            }
        }

        val that = this
        div(className = "search-hints-container") {
            div(className = "search-hints") {
                bindEach(that.state.hints) { hint -> add(SearchHint(hint, that.state)) }

                onEvent {
                    mouseover = {
                        that.mouseOverHints = true
                        that.updateStateFocus()
                    }
                    mouseout = {
                        that.mouseOverHints = false
                        that.updateStateFocus()
                    }
                }
            }

            that.state.showHints.subscribe {
                visible = it
                val clazz = "hints-visible"
                if (it)
                    input.addCssClass(clazz)
                else
                    input.removeCssClass(clazz)
            }
        }
    }

    private fun updateStateFocus() {
        val focusValue = inputHasFocus || mouseOverHints
        if (focusValue != previousFocusValue) {
            state.onFocusChanged(focusValue)
            previousFocusValue = focusValue
        }
    }
}
