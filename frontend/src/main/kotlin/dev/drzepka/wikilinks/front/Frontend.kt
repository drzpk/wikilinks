package dev.drzepka.wikilinks.front

import dev.drzepka.wikilinks.front.component.SearchComponent
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

    override fun start() {
        root("wikilinks", ContainerType.FLUID) {
            header {
                h1("WikiLinks")
            }

            responsiveGridPanel {
                options(1, 1, 6, 3) {
                    add(SearchComponent())
                }
            }
        }
    }
}
