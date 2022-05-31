package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.model.Link
import dev.drzepka.wikilinks.db.Database

class DbLinksRepository(private val database: Database) : LinksRepository {

    override fun getInLinksCount(vararg pageIds: Int): Int {
        val ids = pageIds.map{it.toLong()}
        return database.linksQueries.countInLinks(ids).executeAsOne().SUM!!.toInt()
    }

    override fun getOutLinksCount(vararg pageIds: Int): Int {
        val ids = pageIds.map{it.toLong()}
        return database.linksQueries.countOutLinks(ids).executeAsOne().SUM!!.toInt()
    }

    override fun getInLinks(vararg pageIds: Int): List<Link> {
        val ids = pageIds.map { it.toLong() }
        val rows = database.linksQueries.getInLinks(ids).executeAsList()
        return rows.flatMap { splitInLinks(it.in_links, it.page_id.toInt()) }
    }

    override fun getOutLinks(vararg pageIds: Int): List<Link> {
        val ids = pageIds.map { it.toLong() }
        val rows = database.linksQueries.getOutLinks(ids).executeAsList()
        return rows.flatMap { splitOutLinks(it.page_id.toInt(), it.out_links) }
    }

    private fun splitInLinks(raw: String, to: Int): List<Link> = splitLinks(raw).map { Link(it, to) }

    private fun splitOutLinks(from: Int, raw: String): List<Link> = splitLinks(raw).map { Link(from, it) }

    private fun splitLinks(raw: String): List<Int> {
        if (raw.isEmpty())
            return emptyList()

        return raw.split(',').map { it.toInt() }
    }
}
