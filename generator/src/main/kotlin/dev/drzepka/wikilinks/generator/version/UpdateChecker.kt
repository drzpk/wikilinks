package dev.drzepka.wikilinks.generator.version

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseResolver
import dev.drzepka.wikilinks.common.dump.DumpResolver
import dev.drzepka.wikilinks.common.dump.HttpClientProvider
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.generator.Configuration
import io.ktor.client.engine.apache.*
import kotlinx.coroutines.runBlocking

class UpdateChecker {

    fun getNewVersions(languages: List<DumpLanguage>): Map<DumpLanguage, String> = runBlocking {
        println("Getting Wikipedia dump version info")
        val versionData = languages.associateWith { getLanguageVersionData(it) }

        println("Links database version status:")
        versionData.forEach { (lang, info) ->
            println("  $lang: Current version: ${info.first}, latest version: ${info.second}")
        }

        versionData
            .filter { it.value.second != null }
            .mapValues { it.value.second!! }
    }

    private suspend fun getLanguageVersionData(language: DumpLanguage): Pair<String?, String?> {
        val currentVersion = DatabaseResolver.resolveDatabaseFile(
            Configuration.databasesDirectory!!,
            DatabaseType.LINKS,
            language
        )?.version

        val latestVersion = getLatestDumpVersion(language)
        return currentVersion to latestVersion
    }

    private suspend fun getLatestDumpVersion(language: DumpLanguage): String {
        val resolver = DumpResolver(HttpClientProvider(Apache))
        val latest = resolver.resolveLatestDumps(language)
        return latest.version
    }
}
