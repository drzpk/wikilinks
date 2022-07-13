package dev.drzepka.wikilinks.app.model

data class HistoryEntry(
    val date: Long,
    val version: String,
    val source: Int?,
    val target: Int?,
    val pathCount: Int,
    val degreesOfSeparation: Int,
    val searchDurationPathsMs: Int,
    val searchDurationTotalMs: Int,
    val cacheHitRatio: Double
)
