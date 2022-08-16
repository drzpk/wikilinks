package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

class DbPagesRepository(private val databaseRegistry: DatabaseRegistry) : PagesRepository {

    override suspend fun getPageIds(language: DumpLanguage, titles: Collection<String>): Map<String, Int> {
        val database = databaseRegistry.getLinksDatabase(language)
        val result = database.pagesQueries.getIdsByTitles(titles).executeAsList()
        return result.associate { it.title to it.id.toInt() }
    }

    override suspend fun pagesExist(language: DumpLanguage, pages: List<Int>): List<Boolean> {
        val database = databaseRegistry.getLinksDatabase(language)
        val result = database.pagesQueries.pagesExist(pages.map { it.toLong() }).executeAsList().toSet()
        return pages.map { it.toLong() in result }
    }
}
