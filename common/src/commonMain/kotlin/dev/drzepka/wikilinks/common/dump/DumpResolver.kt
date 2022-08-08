package dev.drzepka.wikilinks.common.dump

import dev.drzepka.wikilinks.common.WikiConfig
import dev.drzepka.wikilinks.common.model.dump.ArchiveDump
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.dump.ResolvedDumps
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class DumpResolver(
    provider: HttpClientProvider,
    private val requiredFileVariants: List<String> = WikiConfig.REQUIRED_FILE_VARIANTS
) {
    private val http = provider.client

    suspend fun resolveLatestDumps(language: DumpLanguage): ResolvedDumps {
        val text = http.get(language.getSourceUrl()).bodyAsText()

        return DATE_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .filter { it != "latest" }
            .sortedWith(reverseOrder())
            .firstNotNullOfOrNull { doResolveForVersion(language, it) }
            ?: throw IllegalStateException("No suitable dump was found")
    }

    suspend fun resolveForVersion(language: DumpLanguage, version: String): ResolvedDumps {
        return doResolveForVersion(language, version)
            ?: throw IllegalArgumentException("Dump for version $version wasn't found.")
    }

    private suspend fun doResolveForVersion(language: DumpLanguage, version: String): ResolvedDumps? {
        val urls = getFileUrls(language, version)
        val dumps = convertToDumps(urls)
        return dumps?.let { ResolvedDumps(version, it) }
    }

    private fun getFileUrls(language: DumpLanguage, version: String): List<String> =
        requiredFileVariants.map { "${language.getSourceUrl()}/$version/${language.value}wiki-$version-$it.sql.gz" }

    private suspend fun convertToDumps(urls: List<String>): List<ArchiveDump>? {
        val resolvedDumps = mutableListOf<ArchiveDump>()

        for (url in urls) {
            val response = http.head(url)
            if (response.status != HttpStatusCode.OK) {
                println("File $url wasn't found")
                return null
            }

            val length = response.contentLength() ?: throw IllegalStateException("Content length is not available")
            val supportsRange = response.headers[HttpHeaders.AcceptRanges]?.equals("bytes", ignoreCase = true)
            resolvedDumps.add(ArchiveDump(url, length, supportsRange ?: false))
        }

        return resolvedDumps
    }

    companion object {
        private val DATE_REGEX = Regex("""<a href="\w+/">(\w+)/</a>""")
    }
}
