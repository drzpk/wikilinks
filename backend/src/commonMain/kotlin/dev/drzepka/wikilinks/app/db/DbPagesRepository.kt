package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.db.links.LinksDatabase

class DbPagesRepository(private val database: LinksDatabase) : PagesRepository {

    override fun getPageTitles(pageIds: Collection<Int>): Map<Int, String> {
        val ids = pageIds.map { it.toLong() }
        val result = database.pagesQueries.getByIds(ids).executeAsList()
        return result.associate { Pair(it.id.toInt(), it.title) }
    }
}
