package dev.drzepka.wikilinks.inttest.search

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object PageInfoMockResponse {

    fun create(vararg pageIds: Int): String {
        val pages = hashMapOf<String, JsonElement>()

        for (pageId in pageIds) {
            val pageContent = mapOf(
                "pageid" to JsonPrimitive(pageId),
                "title" to JsonPrimitive("randomTitle_$pageId"),
            )

            pages[pageId.toString()] = JsonObject(pageContent)
        }

        val pagesJson = Json.encodeToString(pages)
        val
                s = """
            {
                "batchcomplete": "",
                "query": {
                    "pages": $pagesJson
                }
            }
        """
        return s.trimIndent()
    }
}
