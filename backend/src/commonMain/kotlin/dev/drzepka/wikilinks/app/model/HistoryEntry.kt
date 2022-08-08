package dev.drzepka.wikilinks.app.model

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

data class HistoryEntry(
    val date: Long,
    val language: DumpLanguage,
    val version: String,
    val source: Int?,
    val target: Int?,
    val pathCount: Int,
    val degreesOfSeparation: Int,
    val searchDurationPathsMs: Int,
    val searchDurationTotalMs: Int,
    val cacheHitRatio: Double
)
