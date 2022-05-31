package dev.drzepka.wikilinks.front

import io.kvision.Application
import io.kvision.html.div
import io.kvision.html.p
import io.kvision.panel.root

class Frontend : Application() {
    override fun start() {
        root("wikilinks") {
            div {
                p("placeholder")
            }
        }
    }
}
