package dev.drzepka.wikilinks.front.service.impl

import dev.drzepka.wikilinks.common.config.CommonConfiguration
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.front.model.PageHint
import dev.drzepka.wikilinks.front.service.PageSearchService
import dev.drzepka.wikilinks.front.util.http
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.promise
import kotlinx.serialization.json.*
import kotlin.js.Promise

class PageSearchServiceImpl : PageSearchService, CoroutineScope {
    override val coroutineContext = Dispatchers.Default + SupervisorJob()

    override fun search(language: DumpLanguage, title: String, exact: Boolean): Promise<List<PageHint>> {
        return promise { doSearch(language, title, exact) }
    }

    private suspend fun doSearch(language: DumpLanguage, title: String, exact: Boolean): List<PageHint> {
        val apiUrl = CommonConfiguration.wikipediaRestApiUrl(language)
        val response = http.get("$apiUrl/v1/search/page") {
            parameter("limit", if (exact) 1 else 5)
            parameter("q", title)
        }

        var hints = mapResponse(response)
        if (exact && hints.firstOrNull()?.title != title)
            hints = emptyList()

        return hints
    }

    private suspend fun mapResponse(response: HttpResponse): List<PageHint> {
        val obj = response.body<JsonObject>()
        if ("pages" !in obj)
            return emptyList()

        val pages = obj["pages"] as JsonArray
        return pages.map {
            it as JsonObject

            var imageUrl = if ("thumbnail" in it && it["thumbnail"] !is JsonNull)
                it["thumbnail"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
            else null

            if (imageUrl?.startsWith("//") == true)
                imageUrl = "https://$imageUrl"

            PageHint(
                it["id"]!!.jsonPrimitive.int,
                it["title"]!!.jsonPrimitive.content,
                it["description"]?.jsonPrimitive?.contentOrNull ?: "",
                imageUrl
            )
        }
    }
}
