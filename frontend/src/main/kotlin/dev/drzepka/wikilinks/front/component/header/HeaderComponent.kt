package dev.drzepka.wikilinks.front.component.header

import dev.drzepka.wikilinks.front.model.State
import io.kvision.core.Container
import io.kvision.core.Placement
import io.kvision.core.TooltipOptions
import io.kvision.core.enableTooltip
import io.kvision.html.*
import io.kvision.panel.responsiveGridPanel

class HeaderComponent(state: State) : Header() {

    init {
        responsiveGridPanel {
            options(1, 1, 6, 3) {
                h1("WikiLinks")
            }
            options(10, 1, 3) {
                div(className = "right-panel") {
                    add(LanguageSelector(state))
                    this@HeaderComponent.addIcon(this, "https://github.com/drzpk/wikilinks", "GitHub project page")
                }
            }
        }
    }

    private fun addIcon(container: Container, url: String, title: String) {
        container.apply {
            link("", url, className = "icon", target = "_blank") {
                i(className = "bi bi-github")

                enableTooltip(TooltipOptions(title = title, placement = Placement.LEFT, delay = 500, hideDelay = 0))
            }
        }
    }
}
