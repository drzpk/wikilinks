package dev.drzepka.wikilinks.app.model

data class PageCacheHit(val pageId: Int, val urlTitle: String, val displayTitle: String, val description: String)
