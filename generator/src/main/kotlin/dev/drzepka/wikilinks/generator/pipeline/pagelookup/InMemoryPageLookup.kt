package dev.drzepka.wikilinks.generator.pipeline.pagelookup

import com.google.common.collect.HashBiMap

class InMemoryPageLookup  : PageLookup {
    private var pages = HashBiMap.create<Int, String>()

    override fun save(id: Int, title: String) {
        pages[id] = title
    }

    override fun hasId(id: Int): Boolean = id in pages

    override fun getId(title: String): Int? = pages.inverse()[title]

    override fun clear() {
        pages = HashBiMap.create()
    }
}
