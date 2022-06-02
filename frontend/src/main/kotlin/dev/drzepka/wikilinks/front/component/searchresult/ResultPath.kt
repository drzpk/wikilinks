package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.searchresult.PageInfo
import io.kvision.core.JustifyContent
import io.kvision.panel.HPanel

class ResultPath(path: Path, pages: Map<Int, PageInfo>) :
    HPanel(className = "path", justify = JustifyContent.SPACEEVENLY) {

    init {
        for (index in path.pages.indices) {
            add(PathPage(pages[path.pages[index]]!!, index == 0, index == path.pages.size - 1))
        }
    }
}
