package dev.drzepka.wikilinks.common.model.searchresult

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import io.ktor.http.*

@kotlinx.serialization.Serializable
data class PageInfo(
    val id: Int,
    val title: String,
    val url: String,
    val description: String,
    val imageUrl: String? = null
) {
    companion object {
        fun create(
            id: Int,
            title: String,
            description: String,
            imageUrl: String?,
            language: DumpLanguage
        ): PageInfo {
            val url = URLBuilder(
                protocol = URLProtocol.HTTPS,
                host = "${language.value}.wikipedia.org",
                pathSegments = listOf(
                    "wiki",
                    title.replace(" ", "_")
                )
            ).buildString()

            return PageInfo(id, title, url, description, imageUrl)
        }
    }
}
