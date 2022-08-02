package dev.drzepka.wikilinks.app.service

import dev.drzepka.wikilinks.app.db.HistoryRepository
import dev.drzepka.wikilinks.app.model.HistoryEntry
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import kotlinx.datetime.Instant

class HistoryService(
    private val historyRepository: HistoryRepository,
    private val dumpUpdaterService: DumpUpdaterService
) {

    fun addResult(searchDate: Instant, result: LinkSearchResult) {
        val pages = result.paths.firstOrNull()?.pages
        val entry = HistoryEntry(
            searchDate.toEpochMilliseconds(),
            dumpUpdaterService.dumpVersion!!,
            pages?.firstOrNull(),
            pages?.lastOrNull(),
            result.paths.size,
            result.degreesOfSeparation,
            result.duration.graphMs,
            result.duration.totalMs,
            result.cacheHitRatio.toDouble()
        )

        historyRepository.save(entry)
    }
}
