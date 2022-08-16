package dev.drzepka.wikilinks.app.service

import dev.drzepka.wikilinks.app.db.HistoryRepository
import dev.drzepka.wikilinks.app.model.HistoryEntry
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import kotlinx.datetime.Instant

class HistoryService(private val historyRepository: HistoryRepository) {

    suspend fun addResult(searchDate: Instant, result: LinkSearchResult) {
        val entry = HistoryEntry(
            searchDate.toEpochMilliseconds(),
            result.wikipedia.language,
            result.wikipedia.version,
            result.source,
            result.target,
            result.paths.size,
            result.degreesOfSeparation,
            result.duration.graphMs,
            result.duration.totalMs,
            result.cacheHitRatio.toDouble()
        )

        historyRepository.save(entry)
    }
}
