package dev.drzepka.wikilinks.app.search

import dev.drzepka.wikilinks.app.db.PagesRepository
import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.searchresult.PageInfo

class PageInfoService(private val pagesRepository: PagesRepository) {

    fun collectInfo(paths: Collection<Path>): Map<Int, PageInfo> {
        val uniquePageIds = paths
            .asSequence()
            .flatMap { it.pages }
            .toSet()

        val titles = pagesRepository.getPageTitles(uniquePageIds)
        return titles.mapValues { PageInfo(it.value, getPageUrl(it.value)) }
    }

    private fun getPageUrl(title: String): String = "https://en.wikipedia.org/wiki/$title"
}
