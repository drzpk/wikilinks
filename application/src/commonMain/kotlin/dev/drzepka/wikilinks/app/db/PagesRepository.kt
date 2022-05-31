package dev.drzepka.wikilinks.app.db

interface PagesRepository {
    fun getPageTitles(pageIds: Collection<Int>): Map<Int, String>
}
