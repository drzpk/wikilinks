package dev.drzepka.wikilinks.common.model.searchresult

@Suppress("unused")
@kotlinx.serialization.Serializable
data class SearchDuration(val totalMillis: Int, val pathMillis: Int) {
    val otherMillis = totalMillis - pathMillis
}
