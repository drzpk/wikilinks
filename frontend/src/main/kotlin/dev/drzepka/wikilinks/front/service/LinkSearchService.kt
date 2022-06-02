package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult

interface LinkSearchService {
    fun search(sourcePage: Int, targetPage: Int): LinkSearchResult
}
