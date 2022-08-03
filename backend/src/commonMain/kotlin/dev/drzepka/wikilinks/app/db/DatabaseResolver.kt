package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.utils.MultiplatformDirectory
import mu.KotlinLogging

object DatabaseResolver {
    private val log = KotlinLogging.logger {}

    fun resolveDatabaseName(directory: String, type: DatabaseType, language: DumpLanguage? = null): String? =
        resolveDatabaseFile(directory, type, language)?.fileName

    fun resolveDatabaseFile(directory: String, type: DatabaseType, language: DumpLanguage? = null): DatabaseFile? {
        if (type.languageSpecific && language == null)
            throw IllegalArgumentException("Language is required for type $type")

        val filtered = MultiplatformDirectory(directory)
            .listFiles()
            .mapNotNull { DatabaseFile.parse(it.getName()) }
            .filter { it.type == type && it.language == language }

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
}
