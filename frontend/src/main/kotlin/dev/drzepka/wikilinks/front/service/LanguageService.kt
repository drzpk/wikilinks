package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.common.model.LanguageInfo
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlinx.browser.localStorage
import kotlin.js.Promise

interface LanguageService {
    fun getAvailableLanguages(): Promise<List<LanguageInfo>>
    fun getRecentLanguage(): DumpLanguage? = localStorage
        .getItem(RECENT_LANGUAGE_KEY)
        ?.let { DumpLanguage.fromString(it) }

    fun saveRecentLanguage(language: DumpLanguage) = localStorage.setItem(RECENT_LANGUAGE_KEY, language.value)

    companion object {
        private const val RECENT_LANGUAGE_KEY = "RecentLanguage"
    }
}
