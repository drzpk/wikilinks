package dev.drzepka.wikilinks.generator.model

data class Link(val from: Int, val to: Int) {
    operator fun get(index: Int): Int = when(index) {
        0 -> from
        1 -> to
        else -> throw IndexOutOfBoundsException()
    }
}
