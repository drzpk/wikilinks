package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

class DbPagesRepository(private val databaseRegistry: DatabaseRegistry) : PagesRepository {

    override suspend fun getPageId(language: DumpLanguage, title: String): Int? {
        val database = databaseRegistry.getLinksDatabase(language)
        return database.pagesQueries.getIdByTitle(title).executeAsOneOrNull()?.toInt()
    }

    override suspend fun getPageTitles(language: DumpLanguage, pageIds: Collection<Int>): Map<Int, String> {
        val ids = pageIds.map { it.toLong() }
        val database = databaseRegistry.getLinksDatabase(language)
        val result = database.pagesQueries.getByIds(ids).executeAsList()
        return result.associate { Pair(it.id.toInt(), it.title) }
    }
}
