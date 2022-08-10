@file:JvmName("JVMHttpApplication")
package dev.drzepka.wikilinks.app

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*

internal actual fun createEmbeddedServer(port: Int, configuration: Application.() -> Unit) {
    val jvmConfiguration: Application.() -> Unit = {
        install(Compression) {
            gzip()
            deflate()
        }
        configuration()
    }
    embeddedServer(Netty, port = port, module = jvmConfiguration).start(true)
}
