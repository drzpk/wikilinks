package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.searchresult.PageInfo
import io.kvision.html.Div

class ResultPath(path: Path, pages: Map<Int, PageInfo>) : Div(className = "path") {

    init {
        for (index in path.pages.indices) {
            add(PathPage(pages[path.pages[index]]!!, index, path.pages.size))
        }
    }
}
