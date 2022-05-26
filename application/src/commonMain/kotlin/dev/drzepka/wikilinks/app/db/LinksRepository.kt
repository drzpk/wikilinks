package dev.drzepka.wikilinks.app.db

interface LinksRepository {
    fun getOutLinks(pageId: Int): List<Int>
}
