package dev.drzepka.wikilinks.front

import io.kvision.*

fun main() {
    startApplication(::Frontend, module.hot, BootstrapModule, BootstrapCssModule, CoreModule)
}
