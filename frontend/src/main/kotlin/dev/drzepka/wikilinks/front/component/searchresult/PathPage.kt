package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.searchresult.PageInfo
import dev.drzepka.wikilinks.front.util.Images
import io.kvision.core.AlignItems
import io.kvision.core.onClick
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.html.image
import io.kvision.html.p
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import kotlinx.browser.window

class PathPage(info: PageInfo, isFirst: Boolean, isLast: Boolean) : Div(className = "page") {
    init {
        if (isFirst) addCssClass("first")
        if (isLast) addCssClass("last")

        onClick {
            window.open(info.url, target = "_blank")
        }

        if (!isFirst) div(className = "connector left")
        vPanel(className = "content") {
            hPanel(alignItems = AlignItems.CENTER, className = "header") {
                image(info.imageUrl ?: Images.EMPTY)
                p(info.title, className = "title")
            }
            p(info.description, className = "description")
        }
        if (!isLast) div(className = "connector right")
    }
}
