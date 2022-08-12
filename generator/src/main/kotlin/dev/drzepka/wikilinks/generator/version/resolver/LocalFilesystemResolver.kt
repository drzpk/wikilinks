package dev.drzepka.wikilinks.generator.version.resolver

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseResolver
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import java.io.File

class LocalFilesystemResolver(directory: File) : CurrentVersionResolver {
    private val databaseResolver = DatabaseResolver(directory.absolutePath)

    override fun resolve(language: DumpLanguage): String? {
        return databaseResolver.resolveNewestDatabaseFile(DatabaseType.LINKS, language)?.version
    }
}
