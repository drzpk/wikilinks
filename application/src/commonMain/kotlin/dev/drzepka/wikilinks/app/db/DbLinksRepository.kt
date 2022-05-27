package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.model.Link
import dev.drzepka.wikilinks.db.Database

class DbLinksRepository(private val database: Database) : LinksRepository {

    override fun getOutLinks(vararg pageIds:Int): List<Link> {
        val ids = pageIds.map { it.toLong() }
        val rows = database.linksQueries.getOutLinks(ids).executeAsList()
        return rows.flatMap { splitOutLinks(it.page_id.toInt(), it.out_links) }
    }

    private fun splitOutLinks(from: Int, raw: String): List<Link> = splitLinks(raw).map { Link(from, it) }

    private fun splitLinks(raw: String): List<Int> {
        if (raw.isEmpty())
            return emptyList()

        return raw.split(',').map { it.toInt() }
    }
}
