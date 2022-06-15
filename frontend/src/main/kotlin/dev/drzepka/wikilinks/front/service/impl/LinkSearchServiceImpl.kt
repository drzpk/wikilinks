package dev.drzepka.wikilinks.front.service.impl

import dev.drzepka.wikilinks.common.model.LinkSearchRequest
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.front.service.LinkSearchService
import dev.drzepka.wikilinks.front.util.http
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.promise
import kotlin.js.Promise

class LinkSearchServiceImpl : LinkSearchService, CoroutineScope {
    override val coroutineContext = Dispatchers.Default + SupervisorJob()

    override fun search(sourcePage: Int, targetPage: Int): Promise<LinkSearchResult> {
        return promise { doSearch(sourcePage, targetPage) }
    }

    private suspend fun doSearch(sourcePage: Int, targetPage: Int): LinkSearchResult {
        val request = LinkSearchRequest(sourcePage, targetPage)
        val response = http.post("/api/links/search") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }
}
