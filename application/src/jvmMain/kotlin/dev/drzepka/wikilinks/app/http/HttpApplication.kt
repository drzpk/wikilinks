package dev.drzepka.wikilinks.app.http

import dev.drzepka.wikilinks.app.getSearchService
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

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
        route("links") {
            post("search") {
                val request = call.receive<SearchRequest>()
                val result = searchService.search(request.source, request.target)
                call.respond(result)
            }
        }
    }
}