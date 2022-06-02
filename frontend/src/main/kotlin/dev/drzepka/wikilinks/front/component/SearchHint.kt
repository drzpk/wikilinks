package dev.drzepka.wikilinks.front.component

import dev.drzepka.wikilinks.front.model.PageHint
import dev.drzepka.wikilinks.front.model.SearchInputState
import io.kvision.core.onClick
import io.kvision.html.p
import io.kvision.panel.VPanel

class SearchHint(hint: PageHint, searchInputState: SearchInputState) : VPanel(className = "search-hint") {
    init {
        p(hint.title, className = "title")
        p(hint.description, className = "description")
        onClick { searchInputState.selectHint(hint) }
    }
}
