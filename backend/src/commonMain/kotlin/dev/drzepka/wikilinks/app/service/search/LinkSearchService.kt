package dev.drzepka.wikilinks.app.service.search

import dev.drzepka.wikilinks.app.db.PagesRepository
import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.common.model.LanguageInfo
import dev.drzepka.wikilinks.common.model.LinkSearchRequest
import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.common.model.searchresult.SearchDuration
import dev.drzepka.wikilinks.common.utils.sanitizePageTitle
import mu.KotlinLogging
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
class LinkSearchService(
    private val pathFinderService: PathFinderService,
    private val databaseRegistry: DatabaseRegistry,
    private val pageInfoService: PageInfoService?,
    private val pagesRepository: PagesRepository?
) {
    private val log = KotlinLogging.logger {}

    suspend fun search(request: LinkSearchRequest): LinkSearchResult {
        val sourceId = getPageId(request.language, request.source, "source")
        if (sourceId == null) {
            log.debug { "Source page id wasn't found for language ${request.language} and query ${request.source}" }
            return emptySearchResult(request.language)
        }

        val targetId = getPageId(request.language, request.target, "target")
        if (targetId == null) {
            log.debug { "Target page id wasn't found for language ${request.language} and query ${request.target}" }
            return emptySearchResult(request.language)
        }

        return search(
            request.language,
            sourceId,
            targetId
        )
    }

    suspend fun simpleSearch(language: DumpLanguage, sourcePage: Int, targetPage: Int): Pair<List<Path>, Long> {
        val pathTimedValue = measureTimedValue {
            pathFinderService.findPaths(language, sourcePage, targetPage)
        }

        return pathTimedValue.value to pathTimedValue.duration.inWholeMilliseconds
    }

    private suspend fun getPageId(language: DumpLanguage, point: LinkSearchRequest.SearchPoint, name: String): Int? {
        if (point.id != null)
            return point.id!!

        if (pagesRepository == null) {
            log.warn { "Pages repository is null" }
            return null
        }

        if (point.title == null) {
            log.debug { "Neither id nor title is defined for $name" }
            return null
        }

        return pagesRepository.getPageId(language, sanitizePageTitle(point.title!!))
    }

    private suspend fun search(language: DumpLanguage, sourcePage: Int, targetPage: Int): LinkSearchResult {
        if (pageInfoService == null)
            throw IllegalStateException("PageInfoService wasn't found")

        val totalDurationMark = TimeSource.Monotonic.markNow()

        val pathTimedValue = measureTimedValue {
            pathFinderService.findPaths(language, sourcePage, targetPage)
        }

        val paths = pathTimedValue.value
        val pageInfoResult = pageInfoService.collectInfo(language, paths)

        val totalDuration = totalDurationMark.elapsedNow()
        val searchDuration = SearchDuration(
            totalDuration.inWholeMilliseconds.toInt(),
            pathTimedValue.duration.inWholeMilliseconds.toInt(),
            pageInfoResult.pageFetchMs
        )

        return LinkSearchResult(
            paths.firstOrNull()?.let { it.pages.size - 1 } ?: 0,
            paths,
            pageInfoResult.pages,
            searchDuration,
            pageInfoResult.cacheHitRatio,
            language.toDumpInfo()
        )
    }

    private suspend fun emptySearchResult(language: DumpLanguage): LinkSearchResult = LinkSearchResult(
        0,
        emptyList(),
        emptyMap(),
        SearchDuration(0, 0, 0),
        0f,
        language.toDumpInfo()
    )

    private suspend fun DumpLanguage.toDumpInfo(): LanguageInfo = LanguageInfo(
        this,
        databaseRegistry.getAvailableLanguages()[this]!!
    )
}
