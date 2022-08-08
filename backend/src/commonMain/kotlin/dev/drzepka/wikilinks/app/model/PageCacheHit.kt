package dev.drzepka.wikilinks.app.model

data class PageCacheHit(
    val pageId: Int,
    val title: String,
    val description: String,
    val imageUrl: String?
)
