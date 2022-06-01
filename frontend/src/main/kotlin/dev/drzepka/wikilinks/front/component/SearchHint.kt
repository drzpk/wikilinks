package dev.drzepka.wikilinks.front.component

import dev.drzepka.wikilinks.front.model.PageHint
import io.kvision.html.p
import io.kvision.panel.VPanel

class SearchHint(hint: PageHint) : VPanel(className = "search-hint") {
    init {
        p(hint.title, className = "title")
        p(hint.description, className = "description")
    }
}
