package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.searchresult.PageInfo
import dev.drzepka.wikilinks.front.util.Images
import dev.drzepka.wikilinks.front.util.getFillColorForLevel
import io.kvision.core.Background
import io.kvision.core.Color
import io.kvision.html.Link
import io.kvision.html.div
import io.kvision.html.image
import io.kvision.html.p

class PathPage(info: PageInfo, level: Int, levelCount: Int) : Link("", className = "page", target = "_blank") {
    init {
        url = info.url
        div(className = "text") {
            p(info.title, className = "title")
            div(className = "accent") {
                background = Background(this@PathPage.createAccentGradient(level, levelCount))
            }
            p(info.description, className = "description")
        }

        image(
            info.imageUrl ?: Images.EMPTY,
            alt = if (info.imageUrl != null) "Page thumbnail" else "Empty page thumbnail"
        )
    }

    private fun createAccentGradient(level: Int, levelCount: Int): Color {
        val baseColor = getFillColorForLevel(level, levelCount)
        val cutoff = 65
        val value = "linear-gradient(90deg, $baseColor 0%, $baseColor $cutoff%, rgba(255, 255, 255, 0) 100%)"
        return Color(value)
    }
}
