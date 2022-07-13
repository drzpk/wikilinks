package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.model.HistoryEntry
import dev.drzepka.wikilinks.db.history.HistoryDatabase

class DbHistoryRepository(private val historyDatabase: HistoryDatabase) : HistoryRepository {

    override fun save(entry: HistoryEntry) {
        historyDatabase.searchHistoryQueries.add(
            entry.date,
            entry.version,
            entry.source?.toLong(),
            entry.target?.toLong(),
            entry.pathCount.toLong(),
            entry.degreesOfSeparation.toLong(),
            entry.searchDurationPathsMs.toLong(),
            entry.searchDurationTotalMs.toLong(),
            entry.cacheHitRatio
        )
    }
}
