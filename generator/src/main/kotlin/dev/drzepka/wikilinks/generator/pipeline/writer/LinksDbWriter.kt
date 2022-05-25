package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.db.Database
import dev.drzepka.wikilinks.generator.model.PageLinks

class LinksDbWriter(db: Database) : AbstractWriter<PageLinks>(db, 10_000_000) {

    override fun insert(value: PageLinks) {
        db.linksQueries.insert(value.pageId.toLong(), value.inLinks, value.outLinks)
    }
}
