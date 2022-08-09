package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.common.model.searchresult.SearchDuration
import dev.drzepka.wikilinks.front.util.sourcePage
import dev.drzepka.wikilinks.front.util.targetPage
import io.kvision.core.onEvent
import io.kvision.html.*
import kotlin.math.floor

class ResultDescription(result: LinkSearchResult) : Div() {

    init {
        p(className = "result-description") {
            if (!this@ResultDescription.createResultDescription(this, result))
                this@ResultDescription.createEmptyResultDescription(this)
        }
    }

    private fun createResultDescription(p: P, result: LinkSearchResult): Boolean {
        val source = result.sourcePage() ?: return false
        val target = result.targetPage() ?: return false

        p.apply {
            link(source.title, source.url, target = "_blank")
            span(" and ")
            link(target.title, target.url, target = "_blank")

            span(" are connected by ")
            b(result.paths.size.toString())
            span(" unique path")
            if (result.paths.size > 1) span("s")

            span(" with ")
            b(result.degreesOfSeparation.toString())
            span(" degree")
            if (result.degreesOfSeparation > 1) span("s")
            span(" of separation.")
            br()

            val totalFormatted = this@ResultDescription.formatDuration(result.duration.totalMs)
            span("The search took ")
            span(className = "search-duration") {
                val text = span(className = "text") {
                    strong(" $totalFormatted seconds")
                    span(".")
                }
                this@ResultDescription.createSearchDurationDetails(this, text, result.duration)
            }
        }

        return true
    }

    private fun createEmptyResultDescription(p: P) {
        p.apply {
            div(className = "no-paths-found") {
                div("No paths were found between given pages.", className = "header")
                div("Try different search query.", className = "text")
            }
        }
    }

    private fun createSearchDurationDetails(parent: Tag, hoverTrigger: Tag, duration: SearchDuration) {
        parent.apply {
            val container = div(className = "details-container") {
                div(className = "details") {
                    div {
                        span("Graph search: ")
                        strong(this@ResultDescription.formatDuration(duration.graphMs) + " seconds")
                    }
                    div {
                        span("Page info download: ")
                        strong(this@ResultDescription.formatDuration(duration.pageFetchMs) + " seconds")
                    }
                    div {
                        span("Other: ")
                        strong(this@ResultDescription.formatDuration(duration.otherMs) + " seconds")
                    }
                    div {
                        span("Total: ")
                        strong(this@ResultDescription.formatDuration(duration.totalMs) + " seconds")
                    }
                }
            }

            this@ResultDescription.setUpDisplayOnHover(hoverTrigger, container)
        }
    }

    private fun setUpDisplayOnHover(hoveredElement: Tag, displayedElement: Tag) {
        hoveredElement.onEvent {
            mouseenter = {
                displayedElement.addCssClass("visible")
            }

            mouseleave = {
                displayedElement.removeCssClass("visible")
            }
        }
    }

    private fun formatDuration(valueMs: Int): String {
        val raw = floor(valueMs / 10.0 + 0.5).toString().padStart(3, '0')
        return raw.substring(0 until raw.length - 2) + "." + raw.substring(raw.length - 2)
    }
}
