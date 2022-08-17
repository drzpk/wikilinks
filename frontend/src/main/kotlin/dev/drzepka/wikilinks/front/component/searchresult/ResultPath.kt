package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.PageInfo
import dev.drzepka.wikilinks.front.util.ScopedAnalytics
import io.kvision.html.Div

class ResultPath(path: Path, pages: Map<Int, PageInfo>, language: DumpLanguage, analytics: ScopedAnalytics<out Any>) :
    Div(className = "path") {

    init {
        for (index in path.pages.indices) {
            add(PathPage(pages[path.pages[index]]!!, index, path.pages.size, language, analytics))
        }
    }
}
