package dev.drzepka.wikilinks.front.service.impl

import dev.drzepka.wikilinks.common.model.LinkSearchRequest
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
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

    override fun searchByIds(language: DumpLanguage, sourcePage: Int, targetPage: Int): Promise<LinkSearchResult> {
        val source = LinkSearchRequest.SearchPoint(id = sourcePage)
        val target = LinkSearchRequest.SearchPoint(id = targetPage)
        return promise { doSearch(language, source, target) }
    }

    override fun searchByTitles(
        language: DumpLanguage,
        sourcePage: String,
        targetPage: String
    ): Promise<LinkSearchResult> {
        val source = LinkSearchRequest.SearchPoint(title = sourcePage)
        val target = LinkSearchRequest.SearchPoint(title = targetPage)
        return promise { doSearch(language, source, target) }
    }

    private suspend fun doSearch(
        language: DumpLanguage,
        source: LinkSearchRequest.SearchPoint,
        target: LinkSearchRequest.SearchPoint
    ): LinkSearchResult {
        val request = LinkSearchRequest(source, target, language)
        val response = http.post("/api/links/search") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }
}
