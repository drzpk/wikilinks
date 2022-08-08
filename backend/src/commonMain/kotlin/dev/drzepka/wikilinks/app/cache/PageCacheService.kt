package dev.drzepka.wikilinks.app.cache

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.app.model.PageCacheHit
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.db.cache.CacheDatabase
import dev.drzepka.wikilinks.db.cache.schema.FindByIds
import dev.drzepka.wikilinks.db.cache.schema.PageCache
import kotlinx.datetime.Clock
import mu.KotlinLogging

class PageCacheService(private val databaseRegistry: DatabaseRegistry) { // todo: this should be a repository
    private val log = KotlinLogging.logger {}

    suspend fun getCache(language: DumpLanguage, pageIds: Collection<Int>): Map<Int, PageCacheHit> {
        val longPageIds = pageIds.map { it.toLong() }
        val database = databaseRegistry.getCacheDatabase(language)
        val results = database.pageCacheQueries.findByIds(longPageIds).executeAsList()
        log.debug { "Found ${results.size} cache hits for ${pageIds.size} pages: $pageIds" }

        val hits = results.map { createHit(it) }
        updateCacheAccess(database, hits)

        return hits.associateBy { it.pageId }
    }

    suspend fun putCache(language: DumpLanguage, hits: Collection<PageCacheHit>) {
        val now = Clock.System.now().toString()
        log.debug { "Putting new cache: ${hits.size} (date: $now)" }

        val database = databaseRegistry.getCacheDatabase(language)
        database.pageCacheQueries.transaction {
            for (hit in hits) {
                val pageCache = PageCache(
                    hit.pageId.toLong(),
                    now,
                    now,
                    0,
                    hit.title,
                    hit.description,
                    hit.imageUrl
                )

                database.pageCacheQueries.insert(pageCache)
            }
        }
    }

    private fun createHit(result: FindByIds): PageCacheHit =
        PageCacheHit(
            result.page_id.toInt(),
            result.title,
            result.description,
            result.image_url
        )

    private fun updateCacheAccess(database: CacheDatabase, hits: Collection<PageCacheHit>) {
        val now = Clock.System.now().toString()
        val ids = hits.map { it.pageId.toLong() }
        database.pageCacheQueries.updateHits(now, ids)
    }
}
