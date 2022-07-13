package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.app.db.DatabaseProvider
import dev.drzepka.wikilinks.app.db.DbHistoryRepository
import dev.drzepka.wikilinks.app.db.FileConfigRepository
import dev.drzepka.wikilinks.app.service.FrontendResourceService
import dev.drzepka.wikilinks.app.service.HistoryService
import dev.drzepka.wikilinks.common.model.LinkSearchRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock

fun httpApplication() {
    println("Starting HTTP server")

    createEmbeddedServer(port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        install(StatusPages)
        configureRouting()
    }
}

internal expect fun createEmbeddedServer(port: Int, configuration: Application.() -> Unit)

private fun Application.configureRouting() {
    val searchService = getSearchService()
    val frontendResourceService = FrontendResourceService()

    val historyRepository = DbHistoryRepository(DatabaseProvider.getHistoryDatabase())
    val configRepository = FileConfigRepository(Configuration.databasesDirectory!!)
    val historyService = HistoryService(historyRepository, configRepository)

    routing {
        get("/") {
            call.respondRedirect("app/index.html", permanent = false)
        }

        route("app") {
            get("*") {
                val path = call.request.path().substringAfter("/app")
                val resource = frontendResourceService.getResource(path)
                if (resource != null)
                    call.respondBytes(resource.content, resource.contentType)
                else
                    call.respond(HttpStatusCode.NotFound, "")
            }
        }

        route("api") {
            route("links") {
                post("search") {
                    val request = call.receive<LinkSearchRequest>()
                    val searchDate = Clock.System.now()
                    val result = searchService.search(request.source, request.target)
                    historyService.addResult(searchDate, result)
                    call.respond(result)
                }
            }
        }
    }
}