package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.db.links.LinksDatabase
import dev.drzepka.wikilinks.generator.model.PageLinks

class LinksDbWriter(db: LinksDatabase) : AbstractWriter<PageLinks>(db, 1_000_000) {

    override fun insert(value: PageLinks) {
        db.linksQueries.insert(
            value.pageId.toLong(),
            value.inLinksCount.toLong(),
            value.outLinksCount.toLong(),
            value.inLinks,
            value.outLinks
        )
    }
}
