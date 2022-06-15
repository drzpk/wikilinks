package dev.drzepka.wikilinks.common.model.searchresult

@kotlinx.serialization.Serializable
data class PageInfo(val title: String, val url: String, val description: String = "placeholder")
