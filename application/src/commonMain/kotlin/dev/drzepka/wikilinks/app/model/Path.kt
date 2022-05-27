package dev.drzepka.wikilinks.app.model

data class Path(val pages: List<Int>) {
    constructor(vararg pages: Int) : this(pages.asList())

    fun pretty(): String = pages.joinToString(separator = " -> ")
}
