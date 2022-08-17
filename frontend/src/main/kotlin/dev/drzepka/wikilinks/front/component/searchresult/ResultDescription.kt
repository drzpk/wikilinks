package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.common.model.searchresult.SearchDuration
import dev.drzepka.wikilinks.front.util.AnalyticsEvent
import dev.drzepka.wikilinks.front.util.ScopedAnalytics
import dev.drzepka.wikilinks.front.util.sourcePage
import dev.drzepka.wikilinks.front.util.targetPage
import io.kvision.core.onEvent
import io.kvision.html.*
import kotlin.math.floor

class ResultDescription(result: LinkSearchResult, analytics: ScopedAnalytics<out Any>) : Div() {

    init {
        p(className = "result-description") {
            if (!this@ResultDescription.createResultDescription(this, result, analytics))
                this@ResultDescription.createEmptyResultDescription(this)
        }
    }

    private fun createResultDescription(p: P, result: LinkSearchResult, analytics: ScopedAnalytics<out Any>): Boolean {
        val source = result.sourcePage() ?: return false
        val target = result.targetPage() ?: return false

        p.apply {
            link(source.title, source.url, target = "_blank") {
                onClick {
                    val event = AnalyticsEvent.ResultDescriptionLinkClicked(result.wikipedia.language)
                    analytics.triggerEvent(event)
                }
            }
            span(" and ")
            link(target.title, target.url, target = "_blank") {
                onClick {
                    val event = AnalyticsEvent.ResultDescriptionLinkClicked(result.wikipedia.language)
                    analytics.triggerEvent(event)
                }
            }

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
                this@ResultDescription.createSearchDurationDetails(this, text, result.duration, analytics)
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

    private fun createSearchDurationDetails(
        parent: Tag,
        hoverTrigger: Tag,
        duration: SearchDuration,
        analytics: ScopedAnalytics<out Any>
    ) {
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

            this@ResultDescription.setUpDisplayOnHover(hoverTrigger, container, analytics)
        }
    }

    private fun setUpDisplayOnHover(hoveredElement: Tag, displayedElement: Tag, analytics: ScopedAnalytics<out Any>) {
        hoveredElement.onEvent {
            mouseenter = {
                val event = AnalyticsEvent.SearchTimeDetailsShown()
                analytics.triggerEvent(event)
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
