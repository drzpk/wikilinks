package dev.drzepka.wikilinks.app.service.search

import dev.drzepka.wikilinks.app.cache.PageCacheService
import dev.drzepka.wikilinks.app.model.PageCacheHit
import dev.drzepka.wikilinks.app.model.PageInfoResult
import dev.drzepka.wikilinks.app.utils.http
import dev.drzepka.wikilinks.common.config.CommonConfiguration
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.PageInfo
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import mu.KotlinLogging
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
class PageInfoService(private val cacheService: PageCacheService) {
    private val log = KotlinLogging.logger {}

    suspend fun getPagesInfo(language: DumpLanguage, pageIds: Collection<Int>): PageInfoResult {
        val cacheHits = cacheService.getCache(language, pageIds)
        val pagesToDownload = pageIds - cacheHits.keys
        var cacheHitRatio = cacheHits.size.toFloat() / pageIds.size
        if (cacheHitRatio.isNaN())
            cacheHitRatio = 0f

        log.debug {
            val percentage = floor(cacheHitRatio * 1000) / 10
            "Cache hit ratio: ${cacheHits.size}/${pageIds.size} ($percentage%)"
        }

        val (downloadedPages, downloadTime) = fetchPages(language, pagesToDownload)
        cacheService.putCache(language, downloadedPages.values.map { it.toCacheHit() })

        cacheHits
            .asSequence()
            .map { it.value.toPageInfo(language) }
            .forEach { downloadedPages[it.id] = it }

        return PageInfoResult(downloadedPages, cacheHitRatio, downloadTime.inWholeMilliseconds.toInt())
    }

    private fun fetchPages(language: DumpLanguage, pageIds: List<Int>): TimedValue<MutableMap<Int, PageInfo>> {
        if (pageIds.isEmpty()) {
            log.trace { "No pages to download" }
            return TimedValue(mutableMapOf(), Duration.ZERO)
        }

        val value = measureTimedValue {
            downloadPagesFromWikipedia(language, pageIds).associateByTo(HashMap()) { it.id }
        }

        val downloaded = value.value
        log.debug { "Downloaded ${downloaded.size} pages from Wikipedia in ${value.duration.inWholeMilliseconds} ms" }

        if (downloaded.size != pageIds.size) {
            val notFoundPages = pageIds - downloaded.keys
            log.warn { "Some pages weren't found on Wikipedia: $notFoundPages" }
        }

        return TimedValue(downloaded, value.duration)
    }

    private fun downloadPagesFromWikipedia(language: DumpLanguage, pageIds: List<Int>): Collection<PageInfo> {
        // MediaWiki API only allows to query 50 pages per request
        val chunkSize = 50
        val chunks = ceil(pageIds.size / chunkSize.toFloat()).toInt()

        val pagesInfo = ArrayList<PageInfo>()
        for (chunkNo in 0 until chunks) {
            val end = (((chunkNo) + 1) * chunkSize).coerceAtMost(pageIds.size)
            val chunk = pageIds.slice((chunkNo * chunkSize) until end)

            val pages = downloadPagesChunkFromWikipedia(language, chunk)
            pagesInfo.addAll(pages)
        }

        return pagesInfo
    }

    private fun downloadPagesChunkFromWikipedia(language: DumpLanguage, pageIds: Collection<Int>): Collection<PageInfo> {
        // https://www.mediawiki.org/w/api.php?action=help&modules=query
        val obj = runBlocking {
            val response = http.get(CommonConfiguration.wikipediaActionApiUrl(language)) {
                parameter("action", "query")
                parameter("format", "json")
                parameter("prop", "info|pageterms|pageimages")
                parameter("pageids", pageIds.joinToString(separator = "|"))
            }

            response.body<JsonObject>()
        }

        var chunk = emptyList<PageInfo>()
        if ("query" in obj) {
            val queryObj = obj["query"]?.jsonObject!!
            if ("pages" in queryObj) {
                val pagesObj = queryObj["pages"]?.jsonObject!!
                chunk = pagesObj.values.map { it.jsonObject.toPageInfo(language) }
            }
        }

        return chunk
    }

    private fun PageCacheHit.toPageInfo(language: DumpLanguage): PageInfo = PageInfo.create(pageId, title, description, imageUrl, language)

    private fun PageInfo.toCacheHit(): PageCacheHit = PageCacheHit(id, title, description, imageUrl)

    private fun JsonObject.toPageInfo(language: DumpLanguage): PageInfo {
        val id = this["pageid"]?.jsonPrimitive?.intOrNull!!
        val title = this["title"]?.jsonPrimitive?.contentOrNull!!

        var description = ""
        if ("terms" in this) {
            val terms = this["terms"]!!.jsonObject
            if ("description" in terms)
                description = terms["description"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.contentOrNull ?: ""
        }

        var imageUrl: String? = null
        if ("thumbnail" in this) {
            val thumbnail = this["thumbnail"]!!.jsonObject
            if ("source" in thumbnail)
                imageUrl = thumbnail["source"]?.jsonPrimitive?.contentOrNull
        }

        return PageInfo.create(id, title, description, imageUrl, language)
    }
}
