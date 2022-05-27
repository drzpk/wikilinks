package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.model.Link

interface LinksRepository {
    fun getOutLinks(vararg pageIds: Int): List<Link>
}
