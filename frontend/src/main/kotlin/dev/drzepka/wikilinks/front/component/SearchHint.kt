package dev.drzepka.wikilinks.front.component

import dev.drzepka.wikilinks.front.model.PageHint
import dev.drzepka.wikilinks.front.model.SearchInputState
import dev.drzepka.wikilinks.front.util.Images
import io.kvision.core.AlignItems
import io.kvision.core.onClick
import io.kvision.html.div
import io.kvision.html.image
import io.kvision.html.p
import io.kvision.panel.HPanel
import io.kvision.panel.vPanel

class SearchHint(hint: PageHint, searchInputState: SearchInputState) :
    HPanel(className = "search-hint", alignItems = AlignItems.CENTER) {
    init {
        div(className = "image-wrapper") {
            image(
                hint.imageUrl ?: Images.EMPTY,
                alt = if (hint.imageUrl != null) "Page thumbnail" else "Empty page thumbnail"
            )
        }
        vPanel {
            p(hint.title, className = "title")
            p(hint.description, className = "description")
        }
        onClick { searchInputState.selectHint(hint) }
    }
}
