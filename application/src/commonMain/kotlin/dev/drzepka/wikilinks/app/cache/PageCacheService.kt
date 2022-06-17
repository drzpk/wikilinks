package dev.drzepka.wikilinks.app.cache

import dev.drzepka.wikilinks.app.model.PageCacheHit
import dev.drzepka.wikilinks.db.cache.CacheDatabase
import dev.drzepka.wikilinks.db.cache.schema.FindByIds
import dev.drzepka.wikilinks.db.cache.schema.PageCache
import kotlinx.datetime.Clock
import mu.KotlinLogging

class PageCacheService(private val database: CacheDatabase) {
    private val log = KotlinLogging.logger {}

    fun getCache(pageIds: Collection<Int>): Map<Int, PageCacheHit> {
        val longPageIds = pageIds.map { it.toLong() }
        val results = database.pageCacheQueries.findByIds(longPageIds).executeAsList()
        log.debug { "Found ${results.size} cache hits for ${pageIds.size} pages: $pageIds" }

        val hits = results.map { createHit(it) }
        updateCacheAccess(hits)

        return hits.associateBy { it.pageId }
    }

    fun putCache(hits: Collection<PageCacheHit>) {
        val now = Clock.System.now().toString()
        log.debug { "Putting new cache: ${hits.size} (date: $now)" }

        database.pageCacheQueries.transaction {
            for (hit in hits) {
                val pageCache = PageCache(
                    hit.pageId.toLong(),
                    now,
                    now,
                    0,
                    hit.urlTitle,
                    hit.displayTitle,
                    hit.description
                )

                database.pageCacheQueries.insert(pageCache)
            }
        }
    }

    private fun createHit(result: FindByIds): PageCacheHit =
        PageCacheHit(result.page_id.toInt(), result.urlTitle, result.displayTitle, result.description)

    private fun updateCacheAccess(hits: Collection<PageCacheHit>) {
        val now = Clock.System.now().toString()
        val ids = hits.map { it.pageId.toLong() }
        database.pageCacheQueries.updateHits(now, ids)
    }
}
