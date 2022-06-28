package dev.drzepka.wikilinks.common.dump

import io.ktor.client.*
import io.ktor.client.engine.*

class HttpClientProvider private constructor(val client: HttpClient) {
    constructor(factory: HttpClientEngineFactory<*>) : this(HttpClient(factory))
    constructor(engine: HttpClientEngine) : this(HttpClient(engine))
}
