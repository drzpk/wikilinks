package dev.drzepka.wikilinks.front.service.impl

import dev.drzepka.wikilinks.common.model.LanguageInfo
import dev.drzepka.wikilinks.front.service.LanguageService
import dev.drzepka.wikilinks.front.util.http
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.promise
import kotlin.js.Promise

class LanguageServiceImpl : LanguageService, CoroutineScope {
    override val coroutineContext = Dispatchers.Default + SupervisorJob()

    override fun getAvailableLanguages(): Promise<List<LanguageInfo>> = promise { getLanguages() }

    private suspend fun getLanguages(): List<LanguageInfo> {
        val response = http.get("/api/languages")
        return response.body()
    }
}
