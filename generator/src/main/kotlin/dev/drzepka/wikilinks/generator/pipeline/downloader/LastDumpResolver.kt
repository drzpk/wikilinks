package dev.drzepka.wikilinks.generator.pipeline.downloader

import dev.drzepka.wikilinks.generator.Configuration
import dev.drzepka.wikilinks.generator.model.ResolvedDump
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class LastDumpResolver(provider: HttpClientProvider, private val requiredFileVariants: List<String>) {
    private val http = provider.client

    suspend fun resolveLastDumpFileUrls(): List<ResolvedDump> {
        val text = http.get(Configuration.dumpSource).bodyAsText()

        @Suppress("ConvertCallChainIntoSequence")
        val urls = DATE_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .filter { it != "latest" }
            .sortedWith(Comparator.reverseOrder())
            .map { getFileUrls(it) }

        for (url in urls) {
            val dumps = convertToDumps(url)
            if (dumps != null)
                return dumps
        }

        throw IllegalStateException("No suitable dump was found")
    }

    private fun getFileUrls(date: String): List<String> =
        requiredFileVariants.map { "${Configuration.dumpSource}/$date/enwiki-$date-$it.sql.gz" }

    private suspend fun convertToDumps(urls: List<String>): List<ResolvedDump>? {
        val resolvedDumps = mutableListOf<ResolvedDump>()

        for (url in urls) {
            val response = http.head(url)
            if (response.status != HttpStatusCode.OK) {
                println("File $url wasn't found")
                return null
            }

            val length = response.contentLength() ?: throw IllegalStateException("Content length is not available")
            resolvedDumps.add(ResolvedDump(url, length))
        }

        return resolvedDumps
    }

    companion object {
        private val DATE_REGEX = Regex("""<a href="\w+/">(\w+)/</a>""")
    }
}
