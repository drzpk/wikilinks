package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.KoinApp.databaseRegistry
import dev.drzepka.wikilinks.app.KoinApp.frontendResourceService
import dev.drzepka.wikilinks.app.KoinApp.healthService
import dev.drzepka.wikilinks.app.route.linksSearch
import dev.drzepka.wikilinks.app.route.respondWithIndexHtml
import dev.drzepka.wikilinks.common.model.LanguageInfo
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import org.koin.core.context.startKoin

private val log = KotlinLogging.logger("HttpApplication")

fun httpApplication() {
    log.info { "Starting HTTP server" }

    createEmbeddedServer(port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        install(StatusPages) {
            val log = KotlinLogging.logger("exception-handler")
            exception { call: ApplicationCall, e: LanguageNotAvailableException ->
                log.warn { "Unavailable language ${e.language} was requested at ${call.request.uri}" }
                call.respond(HttpStatusCode.BadRequest, "Language ${e.language} is not available")
            }
            exception { call: ApplicationCall, cause: Throwable ->
                log.error(cause) { "Uncaught error while processing request: ${call.request.uri}" }
            }
        }
        configureKoin(this)
        configureRouting()
    }
}

internal expect fun createEmbeddedServer(port: Int, configuration: Application.() -> Unit)

private fun configureKoin(scope: CoroutineScope) {
    startKoin {
        modules(coreModule(), fullModule(scope))
    }
}

private fun Application.configureRouting() {
    routing {
        route("api") {
            route("links") {
                linksSearch()
            }

            get("languages") {
                val response = databaseRegistry
                    .getAvailableLanguages()
                    .map { LanguageInfo(it.key, it.value) }
                call.respond(response)
            }

            get("health") {
                call.respond(healthService.getHealth())
            }

            route("{...}") {
                handle {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        get("/static/{...}") {
            val path = call.request.path().substringAfter("/static")
            if (path == "index.html") {
                respondWithIndexHtml()
            } else {
                val resource = frontendResourceService.getResource(path)
                if (resource != null)
                    call.respondBytes(resource.content, resource.contentType)
                else
                    call.respond(HttpStatusCode.NotFound, "")
            }
        }

        get("{...}") {
            respondWithIndexHtml()
        }

    }
}
