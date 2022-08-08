package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import kotlin.js.Promise

interface LinkSearchService {
    fun searchByIds(language: DumpLanguage, sourcePage: Int, targetPage: Int): Promise<LinkSearchResult>
    fun searchByTitles(language: DumpLanguage, sourcePage: String, targetPage: String): Promise<LinkSearchResult>
}
