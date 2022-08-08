package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

class DbPagesRepository(private val databaseRegistry: DatabaseRegistry) : PagesRepository {

    override suspend fun getPageId(language: DumpLanguage, title: String): Int? {
        val database = databaseRegistry.getLinksDatabase(language)
        return database.pagesQueries.getIdByTitle(title).executeAsOneOrNull()?.toInt()
    }
}
