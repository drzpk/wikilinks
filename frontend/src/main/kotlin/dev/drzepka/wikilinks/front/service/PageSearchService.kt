package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.front.model.PageHint
import kotlin.js.Promise

interface PageSearchService {
    fun search(language: DumpLanguage, title: String, exact: Boolean = false): Promise<List<PageHint>>
}
