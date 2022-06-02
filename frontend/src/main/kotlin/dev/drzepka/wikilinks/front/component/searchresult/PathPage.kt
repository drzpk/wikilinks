package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.searchresult.PageInfo
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.html.p
import io.kvision.panel.vPanel

class PathPage(info: PageInfo, isFirst: Boolean, isLast: Boolean) : Div(className = "page") {
    init {
        if (isFirst) addCssClass("first")
        if (isLast) addCssClass("last")

        if (!isFirst) div(className = "connector left")
        div(className = "content") {
            vPanel {
                p(info.title, className = "title")
                p(info.description, className = "description")
            }
        }
        if (!isLast) div(className = "connector right")
    }
}
