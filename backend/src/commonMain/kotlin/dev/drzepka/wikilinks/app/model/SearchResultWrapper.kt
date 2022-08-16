package dev.drzepka.wikilinks.app.model

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult

data class SearchResultWrapper(
    val result: LinkSearchResult?,
    val source: String,
    val target: String,
    val sourceMissing: Boolean = false,
    val targetMissing: Boolean = false
)
