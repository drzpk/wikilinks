package dev.drzepka.wikilinks.common.model.searchresult

@Suppress("unused")
@kotlinx.serialization.Serializable
data class SearchDuration(val totalMs: Int, val graphMs: Int, val pageFetchMs: Int) {
    val otherMs = totalMs - graphMs
}
