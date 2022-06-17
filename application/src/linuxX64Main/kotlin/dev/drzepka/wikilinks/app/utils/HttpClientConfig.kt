package dev.drzepka.wikilinks.app.utils

import io.ktor.client.engine.*
import io.ktor.client.engine.curl.*

actual fun <T : HttpClientEngineConfig> getHttpEngine(): HttpClientEngineFactory<T> = Curl as HttpClientEngineFactory<T>
