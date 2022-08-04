package dev.drzepka.wikilinks.app.db.infrastructure

import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.utils.MultiplatformDirectory
import mu.KotlinLogging

object DatabaseResolver {
    private val log = KotlinLogging.logger {}
    private val directory = Configuration.databasesDirectory!!

    fun resolveNewestDatabaseFile(type: DatabaseType, language: DumpLanguage? = null): DatabaseFile? {
        val filtered = resolveDatabaseFiles(type, language)
        if (filtered.isEmpty())
            return null

        return if (type.versioned) {
            filtered
                .sortedByDescending { it.version }
                .first()
        } else {
            if (filtered.size > 1)
                log.warn { "There's more than one database of type $type available" }
            filtered.first()
        }
    }

    fun resolveDatabaseFiles(
        typeFilter: DatabaseType? = null,
        languageFilter: DumpLanguage? = null
    ): List<DatabaseFile> = MultiplatformDirectory(directory)
        .listFiles()
        .mapNotNull { DatabaseFile.parse(it.getName()) }
        .filter { (typeFilter == null || it.type == typeFilter) && (languageFilter == null || it.language == languageFilter) }
}
