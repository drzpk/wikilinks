package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.db.links.LinksDatabase
import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.pagelookup.PageLookup

class PageWriter(private val pageLookup: PageLookup, db: LinksDatabase) : AbstractWriter<Value>(db, 1_000_000) {

    override fun filter(value: Value): Boolean {
        // Only store pages with namespace == 0
        return value[1] == 0
    }

    override fun insert(value: List<Any?>) {
        val id = value[0] as Int
        val title = value[2] as String
        val isRedirect = value[4] as Int

        db.pagesQueries.insert(id.toLong(), title, isRedirect.toLong())
        pageLookup.save(id, title)
    }

    override fun finalizeWriting() {
        super.finalizeWriting()
        db.pagesQueries.createIndex()
    }
}
