package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.common.model.LanguageInfo
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlin.js.Promise
import kotlin.random.Random
import kotlin.random.nextInt

object MockLanguageService : LanguageService {
    override fun getAvailableLanguages(): Promise<List<LanguageInfo>> {
        val languages = mutableListOf(DumpLanguage.EN)
        repeat(Random.nextInt(1..DumpLanguage.values().size / 2)) {
            val rand = DumpLanguage.values().random()
            if (rand !in languages)
                languages.add(rand)
        }

        return Promise.resolve(languages.map { LanguageInfo(it, "latest") })
    }
}