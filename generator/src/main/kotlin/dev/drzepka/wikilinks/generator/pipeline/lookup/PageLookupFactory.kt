package dev.drzepka.wikilinks.generator.pipeline.lookup

import dev.drzepka.wikilinks.db.links.LinksDatabase

object PageLookupFactory {
    fun create(db: LinksDatabase): PageLookup {
        return InMemoryPageLookup()
    }
}
