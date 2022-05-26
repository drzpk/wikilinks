package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.db.Database

class DbLinksRepository(private val database: Database) : LinksRepository {
    override fun getOutLinks(pageId: Int): List<Int> {
        val raw = database.linksQueries.getOutLinks(pageId.toLong()).executeAsOneOrNull()
        return splitLinks(raw)
    }

    private fun splitLinks(raw: String?): List<Int> {
        if (raw == null || raw.isEmpty())
            return emptyList()

        return raw.split(',').map { it.toInt() }
    }
}
