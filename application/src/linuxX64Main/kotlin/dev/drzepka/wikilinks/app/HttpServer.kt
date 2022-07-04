package dev.drzepka.wikilinks.app

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

internal actual fun createEmbeddedServer(port: Int, configuration: Application.() -> Unit) {
    embeddedServer(CIO, port = port, module = configuration).start(true)
}
