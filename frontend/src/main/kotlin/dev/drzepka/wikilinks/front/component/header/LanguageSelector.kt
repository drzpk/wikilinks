package dev.drzepka.wikilinks.front.component.header

import dev.drzepka.wikilinks.front.core.Router
import dev.drzepka.wikilinks.front.model.State
import dev.drzepka.wikilinks.front.model.displayName
import dev.drzepka.wikilinks.front.model.flagCss
import io.kvision.dropdown.DropDown
import io.kvision.html.ButtonStyle
import io.kvision.html.link
import io.kvision.html.span
import io.kvision.state.bind
import io.kvision.state.bindEach
import org.w3c.dom.Element

class LanguageSelector(state: State) : DropDown("", style = ButtonStyle.OUTLINEDARK) {
    init {
        id = "language-selector"

        bind(state.selectedLanguage, runImmediately = true) { lang ->
            icon = lang?.flagCss()
            text = lang?.let { "Wikipedia: ${it.displayName()}" } ?: "no language available"

            bindEach(state.availableLanguages) { availableLang ->
                val route = Router.getLanguageRoute(availableLang)

                link(className = "item", label="", url = route.url) {
                    span(className = availableLang.flagCss())
                    span(availableLang.displayName())
                    onClick {
                        it.preventDefault()
                        route.navigate()
                    }
                }
            }
        }

        val buttonElement by lazy { button.getElement()!! as Element }
        val dropdownDivElement by lazy { getElement()?.lastChild!! as Element }
        button.onClick {
            val rect = buttonElement.getBoundingClientRect()
            dropdownDivElement.asDynamic().style.width = rect.width.toString() + "px"
        }
    }
}
