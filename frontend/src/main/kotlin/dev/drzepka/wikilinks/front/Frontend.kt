package dev.drzepka.wikilinks.front

import dev.drzepka.wikilinks.front.component.SearchComponent
import dev.drzepka.wikilinks.front.model.State
import io.kvision.Application
import io.kvision.html.h1
import io.kvision.html.header
import io.kvision.panel.ContainerType
import io.kvision.panel.responsiveGridPanel
import io.kvision.panel.root

class Frontend : Application() {
    init {
        io.kvision.require("./css/app.css")
    }

    private lateinit var state: State

    override fun start(state: Map<String, Any>) {
        this.state = (state["state"] as State?) ?: State()

        root("wikilinks", ContainerType.FLUID) {
            header {
                h1("WikiLinks")
            }

            responsiveGridPanel {
                options(1, 1, 6, 3) {
                    add(SearchComponent(this@Frontend.state))
                }
            }
        }
    }

    override fun dispose(): Map<String, Any> {
        return mapOf(
            "state" to state
        )
    }
}
