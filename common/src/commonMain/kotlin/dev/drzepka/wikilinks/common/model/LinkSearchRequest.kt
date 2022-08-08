package dev.drzepka.wikilinks.common.model

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlinx.serialization.Serializable

@Serializable
data class LinkSearchRequest(
    val source: SearchPoint,
    val target: SearchPoint,
    val language: DumpLanguage = DumpLanguage.EN
) {
    @Serializable
    data class SearchPoint(val id: Int? = null, val title: String? = null)
}
