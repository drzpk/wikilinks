package dev.drzepka.wikilinks.front

import dev.drzepka.wikilinks.front.component.SearchComponent
import dev.drzepka.wikilinks.front.component.searchresult.SearchResultComponent
import dev.drzepka.wikilinks.front.model.State
import dev.drzepka.wikilinks.front.service.MockLinkSearchService
import io.kvision.Application
import io.kvision.html.h1
import io.kvision.html.header
import io.kvision.panel.ContainerType
import io.kvision.panel.responsiveGridPanel
import io.kvision.panel.root

class Frontend : Application() {
    init {
        io.kvision.require("./css/app.css")
        io.kvision.require("./css/loader.css")
    }

    private lateinit var state: State

    override fun start(state: Map<String, Any>) {
        console.log(state)
        this.state = (state["state"] as State?) ?: createState()

        root("wikilinks", ContainerType.FLUID) {
            header {
                h1("WikiLinks")
            }

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

    private fun createState(): State = State(MockLinkSearchService)
}
