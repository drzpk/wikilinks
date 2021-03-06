package dev.drzepka.wikilinks.generator.pipeline.lookup

interface PageLookup {
    fun save(id: Int, title: String)
    fun hasId(id: Int): Boolean
    fun getId(title: String): Int?
    fun clear()
}
