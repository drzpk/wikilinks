package dev.drzepka.wikilinks.common.model.searchresult

@kotlinx.serialization.Serializable
data class PageInfo(val id: Int, val title: String, val urlTitle: String, val description: String = "placeholder") {
    val url = "https://en.wikipedia.org/wiki/$urlTitle"
}
