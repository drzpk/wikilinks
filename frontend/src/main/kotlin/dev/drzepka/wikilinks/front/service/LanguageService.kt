package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.common.model.LanguageInfo
import kotlin.js.Promise

interface LanguageService {
    fun getAvailableLanguages(): Promise<List<LanguageInfo>>
}
