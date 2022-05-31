package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.model.Link

class InMemoryLinksRepository : LinksRepository {
    private val inLinks = HashMap<Int, MutableList<Int>>()
    private val outLinks = HashMap<Int, MutableList<Int>>()

    fun addLinks(pageId: Int, vararg links: Int) {
        if (pageId !in outLinks)
            outLinks[pageId] = ArrayList()

        val outList = outLinks[pageId]!!
        outList.addAll(links.toList())

        for (inLink in links) {
            if (inLink !in inLinks)
                inLinks[inLink] = ArrayList()

            val inList = inLinks[inLink]!!
            inList.add(pageId)
        }
    }

    override fun getInLinksCount(vararg pageIds: Int): Int =
        pageIds.sumOf { page -> inLinks[page]?.size ?: 0 }

    override fun getOutLinksCount(vararg pageIds: Int): Int =
        pageIds.sumOf { page -> outLinks[page]?.size ?: 0 }

    override fun getInLinks(vararg pageIds: Int): List<Link> =
        pageIds.flatMap { to ->
            inLinks[to]?.map { from -> Link(from, to) } ?: emptyList()
        }

    override fun getOutLinks(vararg pageIds: Int): List<Link> =
        pageIds.flatMap { from ->
            outLinks[from]?.map { to -> Link(from, to) } ?: emptyList()
        }
}
