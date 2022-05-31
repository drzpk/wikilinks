package dev.drzepka.wikilinks.app.model.searchresult

import dev.drzepka.wikilinks.app.model.Path

data class LinkSearchResult(
    val degreesOfSeparation: Int,
    val paths: List<Path>,
    val pages: Map<Int, PageInfo>,
    val duration: SearchDuration
)
