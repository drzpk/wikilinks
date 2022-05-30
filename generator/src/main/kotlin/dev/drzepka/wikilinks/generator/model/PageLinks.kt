package dev.drzepka.wikilinks.generator.model

data class PageLinks(
    val pageId: Int,
    val inLinksCount: Int,
    val outLinksCount: Int,
    val inLinks: String,
    val outLinks: String
)
