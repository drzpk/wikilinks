package dev.drzepka.wikilinks.app.service.search

import dev.drzepka.wikilinks.app.db.PagesRepository
import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.app.model.SearchResultWrapper
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

    suspend fun search(request: LinkSearchRequest): SearchResultWrapper {
        val (sourceId, targetId) = getPageIds(request)

        if (sourceId == null || targetId == null)
            return SearchResultWrapper(
                null,
                request.source.getIdOrTitle(),
                request.target.getIdOrTitle(),
                sourceId == null,
                targetId == null
            )

        val result = search(request.language, sourceId, targetId)
        return SearchResultWrapper(result, sourceId.toString(), targetId.toString())
    }

    suspend fun simpleSearch(language: DumpLanguage, sourcePage: Int, targetPage: Int): Pair<List<Path>, Long> {
        val pathTimedValue = measureTimedValue {
            pathFinderService.findPaths(language, sourcePage, targetPage)
        }

        return pathTimedValue.value to pathTimedValue.duration.inWholeMilliseconds
    }

    private suspend fun getPageIds(request: LinkSearchRequest): Pair<Int?, Int?> {
        if (pagesRepository == null) {
            log.warn { "Pages repository is null" }
            return null to null
        }

        val pageIds = when {
            request.source.id != null && request.target.id != null -> verifyPagesExistByIds(
                request.language,
                request.source.id!!,
                request.target.id!!
            )
            request.source.title != null && request.target.title != null -> getPageIdsByTitles(
                request.language,
                request.source.title!!,
                request.target.title!!
            )
            else -> getPageIdsSeparately(request.language, request.source, request.target)
        }

        if (pageIds.first == null)
            log.debug { "Source page id wasn't found for language ${request.language} and query ${request.source}" }
        if (pageIds.second == null)
            log.debug { "Target page id wasn't found for language ${request.language} and query ${request.target}" }

        return pageIds
    }

    private suspend fun verifyPagesExistByIds(language: DumpLanguage, source: Int, target: Int): Pair<Int?, Int?> {
        val exists = pagesRepository!!.pagesExist(language, listOf(source, target))
        return Pair(
            if (exists[0]) source else null,
            if (exists[1]) target else null,
        )
    }

    private suspend fun getPageIdsByTitles(language: DumpLanguage, source: String, target: String): Pair<Int?, Int?> {
        val sanitizedSource = sanitizePageTitle(source)
        val sanitizedTarget = sanitizePageTitle(target)
        val result = pagesRepository!!.getPageIds(language, listOf(sanitizedSource, sanitizedTarget))
        return Pair(
            result[sanitizedSource],
            result[sanitizedTarget]
        )
    }

    private suspend fun getPageIdsSeparately(
        language: DumpLanguage,
        source: LinkSearchRequest.SearchPoint,
        target: LinkSearchRequest.SearchPoint
    ): Pair<Int?, Int?> = Pair(
        getOrVerifyPageId(language, source, "source"),
        getOrVerifyPageId(language, target, "target")
    )

    private suspend fun getOrVerifyPageId(
        language: DumpLanguage,
        point: LinkSearchRequest.SearchPoint,
        name: String
    ): Int? {
        if (point.id != null)
            return if (pagesRepository!!.pagesExist(language, listOf(point.id!!)).first()) point.id!! else null

        if (point.title == null) {
            log.debug { "Neither id nor title is defined for $name" }
            return null
        }

        val sanitizedTitle = sanitizePageTitle(point.title!!)
        return pagesRepository!!.getPageIds(language, listOf(sanitizedTitle))[sanitizedTitle]
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
            sourcePage,
            targetPage,
            paths,
            pageInfoResult.pages,
            searchDuration,
            pageInfoResult.cacheHitRatio,
            language.toDumpInfo()
        )
    }

    private suspend fun DumpLanguage.toDumpInfo(): LanguageInfo = LanguageInfo(
        this,
        databaseRegistry.getAvailableLanguages()[this]!!
    )

    private fun LinkSearchRequest.SearchPoint.getIdOrTitle(): String = id?.toString() ?: title ?: "null"
}
