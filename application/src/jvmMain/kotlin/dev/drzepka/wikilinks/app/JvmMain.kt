package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.http.httpApplication

fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "http")
        httpApplication()
    else
        cmdLineSearch(args)
}