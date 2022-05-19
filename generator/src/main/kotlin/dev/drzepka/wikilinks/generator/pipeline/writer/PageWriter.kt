package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.db.Database

class PageWriter(db: Database) : AbstractWriter(db) {
    val pages = HashMap<Int, String>()

    override fun insert(value: List<Any?>) {
        val id = value[0] as Int
        val title = value[2] as String
        val isRedirect = value[4] as Int

        db.pagesQueries.insert(id.toLong(), title, isRedirect.toLong())
        pages[id] = title
    }

    override fun finalizeWriting() {
        super.finalizeWriting()
        db.pagesQueries.createIndex()
    }
}
