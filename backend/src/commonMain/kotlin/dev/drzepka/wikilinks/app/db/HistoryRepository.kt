package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.model.HistoryEntry

interface HistoryRepository {
    fun save(entry: HistoryEntry)
}
