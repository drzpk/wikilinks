package dev.drzepka.wikilinks.common.model.searchresult

import dev.drzepka.wikilinks.common.model.LanguageInfo
import dev.drzepka.wikilinks.common.model.Path

@kotlinx.serialization.Serializable
data class LinkSearchResult(
    val degreesOfSeparation: Int,
    val paths: List<Path>,
    val pages: Map<Int, PageInfo>,
    val duration: SearchDuration,
    val cacheHitRatio: Float,
    val wikipedia: LanguageInfo
)
