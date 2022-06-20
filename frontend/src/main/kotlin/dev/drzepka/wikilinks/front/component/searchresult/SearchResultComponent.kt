package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import io.kvision.core.Display
import io.kvision.html.Div
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.state.ObservableState
import io.kvision.state.ObservableValue
import io.kvision.state.bind
import io.kvision.state.bindEach

class SearchResultComponent(result: ObservableState<LinkSearchResult?>) : Div(className = "search-result") {
    private val pathDisplayIncrement = 25
    private var displayedPaths = ObservableValue(listOf<Path>())

    init {
        bind(result) {
            if (it != null) {
                displayedPaths.setState(it.paths.take(pathDisplayIncrement))

                add(ResultDescription(it))
                div("Found paths:", className = "found-paths-text")
                div(className = "paths").bindEach(displayedPaths) { path ->
                    add(ResultPath(path, it.pages))
                }

                div(className = "more-paths") {
                    if (this@SearchResultComponent.shouldShowMorePathsButton(it))
                        this@SearchResultComponent.addButton(this, it)
                }
            }
        }
    }

    private fun addButton(div: Div, result: LinkSearchResult) {
        div.apply {
            button("Load more paths") {
                onClick {
                    val that = this@SearchResultComponent
                    val displayedSize = that.displayedPaths.value.size
                    if (displayedSize < result.paths.size) {
                        val newSize = displayedSize + that.pathDisplayIncrement
                        that.displayedPaths.setState(result.paths.take(newSize))
                    }

                    if (!this@SearchResultComponent.shouldShowMorePathsButton(result)) {
                        div.display = Display.NONE
                    }
                }
            }
        }
    }

    private fun shouldShowMorePathsButton(result: LinkSearchResult): Boolean =
        displayedPaths.value.size < result.paths.size
}
