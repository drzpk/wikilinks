package dev.drzepka.wikilinks.generator.pipeline.downloader

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*

class HttpClientProvider private constructor(val client: HttpClient) {

    constructor(factory: HttpClientEngineFactory<*>) : this(HttpClient(factory))

    constructor(engine: HttpClientEngine) : this(HttpClient(engine))

    companion object {
        fun create(): HttpClientProvider = HttpClientProvider(Apache)
    }
}
