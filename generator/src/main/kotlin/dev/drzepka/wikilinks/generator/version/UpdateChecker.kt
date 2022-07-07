package dev.drzepka.wikilinks.generator.version

import dev.drzepka.wikilinks.app.db.ConfigRepository
import dev.drzepka.wikilinks.common.dump.DumpResolver
import dev.drzepka.wikilinks.common.dump.HttpClientProvider
import io.ktor.client.engine.apache.*
import kotlinx.coroutines.runBlocking

class UpdateChecker(private val configRepository: ConfigRepository) {

    fun getNewVersion(): String? = runBlocking {
        val currentVersion = configRepository.getDumpVersion()
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
