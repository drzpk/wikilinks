package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.model.Link

interface LinksRepository {
    fun getInLinksCount(vararg pageIds: Int): Int
    fun getOutLinksCount(vararg pageIds: Int): Int
    fun getInLinks(vararg pageIds: Int): List<Link>
    fun getOutLinks(vararg pageIds: Int): List<Link>
}
