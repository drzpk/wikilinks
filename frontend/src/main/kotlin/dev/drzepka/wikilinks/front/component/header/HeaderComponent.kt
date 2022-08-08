package dev.drzepka.wikilinks.front.component.header

import dev.drzepka.wikilinks.front.model.State
import io.kvision.html.Header
import io.kvision.html.h1
import io.kvision.panel.responsiveGridPanel

class HeaderComponent(state: State) : Header() {

    init {
        responsiveGridPanel {
            options(1, 1, 6, 3) {
                h1("WikiLinks")
            }
            options(10, 1, 3) {
                add(LanguageSelector(state))
            }
        }
    }
}
