package dev.drzepka.wikilinks.app.utils

import dev.drzepka.wikilinks.common.WikiConfig
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*

val http = HttpClient(getHttpEngine()) {
    install(ContentNegotiation) {
        json()
    }

    defaultRequest {
        headers {
            append(WikiConfig.USER_AGENT_HEADER, WikiConfig.USER_AGENT_HEADER_VALUE)
        }
    }
}

expect fun getHttpEngine(): HttpClientEngineFactory<HttpClientEngineConfig>
