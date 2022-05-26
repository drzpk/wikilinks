package dev.drzepka.wikilinks.app.db

class InMemoryLinksRepository : LinksRepository {
    private val outLinks = HashMap<Int, MutableList<Int>>()

    fun addOutLinks(pageId: Int, vararg links: Int) {
        if (pageId !in outLinks)
            outLinks[pageId] = ArrayList()

        val list = outLinks[pageId]!!
        list.addAll(links.toList())
    }

    override fun getOutLinks(pageId: Int): List<Int> = outLinks[pageId] ?: emptyList()
}
