package dev.drzepka.wikilinks.generator.pipeline.downloader

import dev.drzepka.wikilinks.generator.Configuration
import dev.drzepka.wikilinks.generator.model.ArchiveDump
import dev.drzepka.wikilinks.generator.model.ResolvedDumps
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class LastDumpResolver(provider: HttpClientProvider, private val requiredFileVariants: List<String>) {
    private val http = provider.client

    suspend fun resolveLastDumpFileUrls(): ResolvedDumps {
        val text = http.get(Configuration.dumpSource).bodyAsText()

        @Suppress("ConvertCallChainIntoSequence")
        val versions = DATE_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .filter { it != "latest" }
            .sortedWith(Comparator.reverseOrder())

        for (version in versions) {
            val urls = getFileUrls(version)
            val dumps = convertToDumps(urls)
            if (dumps != null)
                return ResolvedDumps(version, dumps)
        }

        throw IllegalStateException("No suitable dump was found")
    }

    private fun getFileUrls(version: String): List<String> =
        requiredFileVariants.map { "${Configuration.dumpSource}/$version/enwiki-$version-$it.sql.gz" }

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
