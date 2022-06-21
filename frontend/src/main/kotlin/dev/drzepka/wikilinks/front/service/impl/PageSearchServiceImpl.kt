package dev.drzepka.wikilinks.front.service.impl

import dev.drzepka.wikilinks.common.WikiConfig
import dev.drzepka.wikilinks.front.model.PageHint
import dev.drzepka.wikilinks.front.service.PageSearchService
import dev.drzepka.wikilinks.front.util.http
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.promise
import kotlinx.serialization.json.*
import kotlin.js.Promise

class PageSearchServiceImpl : PageSearchService, CoroutineScope {
    override val coroutineContext = Dispatchers.Default + SupervisorJob()

    override fun search(title: String): Promise<List<PageHint>> {
        return promise { doSearch(title) }
    }

    private suspend fun doSearch(title: String): List<PageHint> {
        val response = http.get("${WikiConfig.REST_API_URL}/v1/search/page") {
            parameter("limit", 5)
            parameter("q", title)
        }

        val obj = response.body<JsonObject>()
        if ("pages" !in obj)
            return emptyList()

        val pages = obj["pages"] as JsonArray
        return pages.map {
            it as JsonObject

            val imageUrl = if ("thumbnail" in it && it["thumbnail"] !is JsonNull)
                it["thumbnail"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
            else null

            PageHint(
                it["id"]!!.jsonPrimitive.int,
                it["title"]!!.jsonPrimitive.content,
                it["description"]?.jsonPrimitive?.contentOrNull ?: "",
                imageUrl
            )
        }
    }
}
