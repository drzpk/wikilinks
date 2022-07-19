@file:JvmName("HttpClientConfigJVM")
package dev.drzepka.wikilinks.app.utils

import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*

actual fun getHttpEngine(): HttpClientEngineFactory<HttpClientEngineConfig> = Apache
