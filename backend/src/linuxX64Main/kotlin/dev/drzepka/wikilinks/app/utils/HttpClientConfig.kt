package dev.drzepka.wikilinks.app.utils

import io.ktor.client.engine.*
import io.ktor.client.engine.curl.*

actual fun getHttpEngine(): HttpClientEngineFactory<HttpClientEngineConfig> = Curl
