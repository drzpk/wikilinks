package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.front.util.sourcePage
import dev.drzepka.wikilinks.front.util.targetPage
import io.kvision.html.*

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
            strong { link(source.title, source.url) }
            span(" and ")
            strong { link(target.title, target.url) }

            span(" are connected by ")
            strong { span(result.paths.size.toString()) }
            span(" unique path")
            if (result.paths.size > 1) span("s")

            span(" with ")
            strong { span(result.degreesOfSeparation.toString()) }
            span(" degree")
            if (result.degreesOfSeparation > 1) span("s")
            span(" of separation.")
        }

        return true
    }

    private fun createEmptyResultDescription(p: P) {
        p.apply {
            span("No paths were found between given pages.")
        }
    }
}
