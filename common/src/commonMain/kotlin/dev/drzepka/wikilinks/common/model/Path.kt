package dev.drzepka.wikilinks.common.model

@kotlinx.serialization.Serializable
data class Path(val pages: List<Int>) {
    constructor(vararg pages: Int) : this(pages.asList())

    fun pretty(): String = pages.joinToString(separator = " -> ")
}
