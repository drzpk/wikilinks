package dev.drzepka.wikilinks.app.route

import dev.drzepka.wikilinks.app.KoinApp.historyService
import dev.drzepka.wikilinks.app.KoinApp.searchService
import dev.drzepka.wikilinks.common.model.LinkSearchRequest
import dev.drzepka.wikilinks.common.model.error.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock

fun Route.linksSearch() {
    post("search") {
        val request = call.receive<LinkSearchRequest>()
        val searchDate = Clock.System.now()
        val wrapper = searchService.search(request)
        if (wrapper.result != null) {
            historyService.addResult(searchDate, wrapper.result)
            call.respond(wrapper.result)
        } else {
            val error = when {
                wrapper.sourceMissing && wrapper.targetMissing -> ErrorResponse.sourceAndTargetPageNotFound(
                    wrapper.source,
                    wrapper.target
                )
                wrapper.sourceMissing -> ErrorResponse.sourcePageNotFound(wrapper.source)
                else -> ErrorResponse.targetPageNotFound(wrapper.target)
            }
            call.respond(HttpStatusCode.NotFound, error)
        }
    }
}
