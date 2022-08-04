package dev.drzepka.wikilinks.app.service.search

import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.common.model.searchresult.SearchDuration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
class LinkSearchService(
    private val pathFinderService: PathFinderService,
    private val pageInfoService: PageInfoService?
) {

    suspend fun search(language: DumpLanguage, sourcePage: Int, targetPage: Int): LinkSearchResult {
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
            pageInfoResult.cacheHitRatio
        )
    }

    suspend fun simpleSearch(language: DumpLanguage, sourcePage: Int, targetPage: Int): Pair<List<Path>, Long> {
        val pathTimedValue = measureTimedValue {
            pathFinderService.findPaths(language, sourcePage, targetPage)
        }

       return pathTimedValue.value to pathTimedValue.duration.inWholeMilliseconds
    }
}
