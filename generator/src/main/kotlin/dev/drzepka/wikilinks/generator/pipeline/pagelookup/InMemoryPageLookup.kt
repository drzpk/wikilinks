package dev.drzepka.wikilinks.generator.pipeline.pagelookup

import com.google.common.collect.HashBiMap

class InMemoryPageLookup  : PageLookup {
    // Title lookup yields no result approx. 9000 times more often compared to id lookup below,
    // so it makes sense to have it as a key.
    private var pages = HashBiMap.create<String, Int>()

    override fun save(id: Int, title: String) {
        pages[title] = id
    }

    override fun hasId(id: Int): Boolean = id in pages.inverse()

    override fun getId(title: String): Int? = pages[title]

    override fun clear() {
        pages = HashBiMap.create()
    }
}
