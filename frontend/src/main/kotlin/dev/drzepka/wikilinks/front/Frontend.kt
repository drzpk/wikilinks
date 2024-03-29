package dev.drzepka.wikilinks.front

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.front.component.CookieConsent
import dev.drzepka.wikilinks.front.component.SearchComponent
import dev.drzepka.wikilinks.front.component.header.HeaderComponent
import dev.drzepka.wikilinks.front.component.searchresult.SearchResultComponent
import dev.drzepka.wikilinks.front.core.Router
import dev.drzepka.wikilinks.front.model.State
import dev.drzepka.wikilinks.front.model.displayName
import dev.drzepka.wikilinks.front.service.MockLanguageService
import dev.drzepka.wikilinks.front.service.MockLinkSearchService
import dev.drzepka.wikilinks.front.service.MockPageSearchService
import dev.drzepka.wikilinks.front.service.impl.LanguageServiceImpl
import dev.drzepka.wikilinks.front.service.impl.LinkSearchServiceImpl
import dev.drzepka.wikilinks.front.service.impl.PageSearchServiceImpl
import io.kvision.Application
import io.kvision.panel.ContainerType
import io.kvision.panel.responsiveGridPanel
import io.kvision.panel.root
import kotlinx.browser.document
import org.w3c.dom.HTMLMetaElement

class Frontend : Application() {
    init {
        io.kvision.require("./css/root.scss")
        io.kvision.require("bootstrap-icons/font/bootstrap-icons.css")
        io.kvision.require("flag-icons/css/flag-icons.min.css")
    }

    private lateinit var state: State

    override fun start(state: Map<String, Any>) {
        console.log(state)
        this.state = (state["state"] as State?) ?: createState()
        Router.initialize(this.state)
        setupStateListeners()

        root("wikilinks", ContainerType.FLUID) {
            add(CookieConsent())
            add(HeaderComponent(this@Frontend.state))

            responsiveGridPanel {
                options(1, 1, 12, 0) {
                    add(SearchComponent(this@Frontend.state))
                }

                options(1, 2, 12, 0) {
                    add(SearchResultComponent(this@Frontend.state.searchResult, this@Frontend.state.analytics))
                }
            }
        }
    }

    override fun dispose(): Map<String, Any> {
        // todo: this doesn't seem to be working
        return mapOf(
            "state" to state
        )
    }

    private fun createState(): State {
        val pageSearchService = if (USE_MOCKS) MockPageSearchService else PageSearchServiceImpl()
        val linkSearchService = if (USE_MOCKS) MockLinkSearchService else LinkSearchServiceImpl()
        val languageService = if (USE_MOCKS) MockLanguageService else LanguageServiceImpl()

        return State(pageSearchService, linkSearchService, languageService, Router)
    }

    private fun setupStateListeners() {
        state.selectedLanguage.subscribe {
            updatePageSummary(it, state.searchResult.value)
        }

        state.searchResult.subscribe {
            updatePageSummary(state.selectedLanguage.value, it)
        }
    }

    private fun updatePageSummary(language: DumpLanguage?, searchResult: LinkSearchResult?) {
        if (language == null)
            return

        if (searchResult == null) {
            document.title = "WikiLinks - find connections between articles on ${language.displayName()} Wikipedia"
            setDescriptionMetaTag(
                """
                    Find shortest connections between two ${language.displayName()} Wikipedia articles. 
                    The website implements the concept of six degrees of separation.
                """.trimIndent()
            )
        } else updatePageSummaryWithResult(language,  searchResult)
    }

    private fun updatePageSummaryWithResult(language: DumpLanguage, searchResult: LinkSearchResult) {
        val source = searchResult.pages[searchResult.source]?.title!!
        val target = searchResult.pages[searchResult.target]?.title!!

        document.title = "$source - $target | WikiLinks"
        if (searchResult.paths.isNotEmpty()) {
            val degreeText = if (searchResult.degreesOfSeparation == 1) "1 degree" else "${searchResult.degreesOfSeparation} degrees"
            val pathText = if (searchResult.paths.size == 1) "Only 1 path was found" else "${searchResult.paths.size} paths were found"
            setDescriptionMetaTag("$source and $target are connected by $degreeText of separation. $pathText on ${language.displayName()} Wikipedia.")
        } else {
            setDescriptionMetaTag("No paths were found between $source and $target on ${language.displayName()} Wikipedia.")
        }
    }

    private fun setDescriptionMetaTag(value: String) = setMetaTag("description", value)

    private fun setMetaTag(name: String, content: String) {
        val children = document.head?.childNodes ?: return
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is HTMLMetaElement && child.name == name) {
                child.content = content.replace("\n", "")
                break
            }
        }
    }

    companion object {
        private const val USE_MOCKS = false
    }
}
