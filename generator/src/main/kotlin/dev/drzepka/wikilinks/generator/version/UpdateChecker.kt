package dev.drzepka.wikilinks.generator.version

import dev.drzepka.wikilinks.common.dump.DumpResolver
import dev.drzepka.wikilinks.common.dump.HttpClientProvider
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.generator.Configuration
import dev.drzepka.wikilinks.generator.utils.getFilePath
import dev.drzepka.wikilinks.generator.version.resolver.CurrentVersionResolver
import dev.drzepka.wikilinks.generator.version.resolver.LocalFilesystemResolver
import dev.drzepka.wikilinks.generator.version.resolver.VersionManifestResolver
import io.ktor.client.engine.apache.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URI

class UpdateChecker(workingDirectory: File) {
    private val versionManifestResolver = VersionManifestResolver(workingDirectory)
    private val resolver = createVersionResolver() ?: versionManifestResolver

    fun getNewVersions(languages: List<DumpLanguage>): Map<DumpLanguage, String> = runBlocking {
        println("Getting Wikipedia dump version info")
        val versionData = languages.associateWith { getLanguageVersionData(it) }

        println("Links database version status:")
        versionData.forEach { (lang, info) ->
            println("  $lang: Current version: ${info.first}, latest version: ${info.second}")
        }

        versionData
            .filter { it.value.first != null }
            .forEach { setVersion(it.key, it.value.first!!) }

        versionData
            .filter { it.value.first != it.value.second }
            .mapValues { it.value.second }
    }

    fun setVersion(language: DumpLanguage, version: String) = versionManifestResolver.setVersion(language, version)

    private suspend fun getLanguageVersionData(language: DumpLanguage): Pair<String?, String> {
        val currentVersion = resolver.resolve(language)
        val latestVersion = getLatestDumpVersion(language)
        return currentVersion to latestVersion
    }

    private suspend fun getLatestDumpVersion(language: DumpLanguage): String {
        val resolver = DumpResolver(HttpClientProvider(Apache))
        val latest = resolver.resolveLatestDumps(language)
        return latest.version
    }

    private fun createVersionResolver(): CurrentVersionResolver? {
        val rawUri = Configuration.currentVersionLocation ?: return null
        val uri = URI.create(rawUri)

        return when (uri.scheme) {
            "file" -> {
                val path = uri.getFilePath()
                LocalFilesystemResolver(path)
            }
            else -> throw IllegalArgumentException("Unsupported output uri scheme: ${uri.scheme}")
        }
    }
}
