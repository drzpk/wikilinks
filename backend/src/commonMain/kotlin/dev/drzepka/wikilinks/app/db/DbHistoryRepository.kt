package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.app.model.HistoryEntry

class DbHistoryRepository(databaseRegistry: DatabaseRegistry) : HistoryRepository {
    private val historyDatabase = databaseRegistry.getHistoryDatabase()

    override suspend fun save(entry: HistoryEntry) {
        historyDatabase.searchHistoryQueries.add(
            entry.date,
            entry.language.value,
            entry.version,
            entry.source.toLong(),
            entry.target.toLong(),
            entry.pathCount.toLong(),
            entry.degreesOfSeparation.toLong(),
            entry.searchDurationPathsMs.toLong(),
            entry.searchDurationTotalMs.toLong(),
            entry.cacheHitRatio
        )
    }
}
