package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.model.Link

class InMemoryLinksRepository : LinksRepository {
    private val outLinks = HashMap<Int, MutableList<Int>>()

    fun addOutLinks(pageId: Int, vararg links: Int) {
        if (pageId !in outLinks)
            outLinks[pageId] = ArrayList()

        val list = outLinks[pageId]!!
        list.addAll(links.toList())
    }

    override fun getOutLinks(vararg pageIds: Int): List<Link> {
        return pageIds.flatMap { from ->
            outLinks[from]?.map { to -> Link(from, to) } ?: emptyList()
        }
    }
}
