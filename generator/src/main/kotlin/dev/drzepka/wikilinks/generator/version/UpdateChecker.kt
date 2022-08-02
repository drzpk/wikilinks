package dev.drzepka.wikilinks.generator.version

import dev.drzepka.wikilinks.app.db.DatabaseResolver
import dev.drzepka.wikilinks.common.dump.DumpResolver
import dev.drzepka.wikilinks.common.dump.HttpClientProvider
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.generator.Configuration
import io.ktor.client.engine.apache.*
import kotlinx.coroutines.runBlocking

class UpdateChecker {

    fun getNewVersion(): String? = runBlocking {
        val currentVersion =
            DatabaseResolver.resolveDatabaseFile(Configuration.databasesDirectory!!, DatabaseType.LINKS)?.version
        val latestVersion = getLatestDumpVersion()

        println("Current version: $currentVersion, latest version: $latestVersion")

        if (currentVersion != latestVersion)
            latestVersion
        else null
    }

    private suspend fun getLatestDumpVersion(): String {
        val resolver = DumpResolver.createFromConfig(HttpClientProvider(Apache))
        val latest = resolver.resolveLatestDumps()
        return latest.version
    }
}
