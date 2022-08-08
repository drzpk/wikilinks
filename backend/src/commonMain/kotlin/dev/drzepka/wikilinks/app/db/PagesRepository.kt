package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

interface PagesRepository {
    suspend fun getPageId(language: DumpLanguage, title: String): Int?
}
