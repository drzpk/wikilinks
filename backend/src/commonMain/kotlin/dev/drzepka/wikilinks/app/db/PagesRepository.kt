package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

interface PagesRepository {
    suspend fun getPageIds(language: DumpLanguage, titles: Collection<String>): Map<String, Int>
    suspend fun pagesExist(language: DumpLanguage, pages: List<Int>): List<Boolean>
}
