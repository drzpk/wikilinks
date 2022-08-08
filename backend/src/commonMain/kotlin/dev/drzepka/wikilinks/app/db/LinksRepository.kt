package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.model.Link
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

interface LinksRepository {
    suspend fun getInLinksCount(language: DumpLanguage, vararg pageIds: Int): Int
    suspend fun getOutLinksCount(language: DumpLanguage, vararg pageIds: Int): Int
    suspend fun getInLinks(language: DumpLanguage, vararg pageIds: Int): List<Link>
    suspend fun getOutLinks(language: DumpLanguage, vararg pageIds: Int): List<Link>
}
