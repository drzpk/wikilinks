package dev.drzepka.wikilinks.app.model.searchresult

@Suppress("unused")
data class SearchDuration(val totalMillis: Int, val pathMillis: Int) {
    val otherMillis: Int
        get() = totalMillis - pathMillis
}
