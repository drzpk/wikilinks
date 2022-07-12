package dev.drzepka.wikilinks.generator.pipeline.lookup

interface RedirectLookup {
    operator fun get(from: Int): Int?
    operator fun set(from: Int, to: Int)
    fun clear()
}
