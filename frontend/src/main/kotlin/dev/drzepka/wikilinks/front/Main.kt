package dev.drzepka.wikilinks.front

import io.kvision.BootstrapModule
import io.kvision.CoreModule
import io.kvision.module
import io.kvision.startApplication

fun main() {
    // Don't include BootstrapCssModule directly, it will be loaded manually from a SCSS file
    // to allow customizations (https://getbootstrap.com/docs/5.2/customize/overview/).
    startApplication(::Frontend, module.hot, BootstrapModule, CoreModule)
}
