@file:JvmName("JVMHttpApplication")
package dev.drzepka.wikilinks.app

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

internal actual fun createEmbeddedServer(port: Int, configuration: Application.() -> Unit) {
    embeddedServer(Netty, port = port, module = configuration).start(true)
}
