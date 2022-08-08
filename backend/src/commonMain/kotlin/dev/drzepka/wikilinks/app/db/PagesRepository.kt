package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

interface PagesRepository {
    suspend fun getPageTitles(language: DumpLanguage, pageIds: Collection<Int>): Map<Int, String>
}
