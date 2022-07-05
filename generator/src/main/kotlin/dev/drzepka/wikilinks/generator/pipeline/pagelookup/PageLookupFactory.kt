package dev.drzepka.wikilinks.generator.pipeline.pagelookup

import dev.drzepka.wikilinks.db.links.LinksDatabase

object PageLookupFactory {
    fun create(db: LinksDatabase): PageLookup {
        return InMemoryPageLookup()
    }
}
