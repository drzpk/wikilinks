package dev.drzepka.wikilinks.app.http

import dev.drzepka.wikilinks.app.getSearchService
import dev.drzepka.wikilinks.common.model.LinkSearchRequest
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun httpApplication() {
    println("Starting HTTP server")

    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages)

        configureRouting()
    }.start(wait = true)
}

private fun Application.configureRouting() {
    val searchService = getSearchService()

    routing {
        route("api") {
            route("links") {
                post("search") {
                    val request = call.receive<LinkSearchRequest>()
                    val result = searchService.search(request.source, request.target)
                    call.respond(result)
                }
            }
        }
    }
}