package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.db.Database
import dev.drzepka.wikilinks.generator.model.Value

class PageWriter(db: Database) : AbstractWriter<Value>(db, 1_000_000) {
    val pages = HashMap<String, Int>()

    override fun filter(value: Value): Boolean {
        // Only store pages with namespace == 0
        return value[1] == 0
    }

    override fun insert(value: List<Any?>) {
        val id = value[0] as Int
        val title = value[2] as String
        val isRedirect = value[4] as Int

        db.pagesQueries.insert(id.toLong(), title, isRedirect.toLong())
        pages[title] = id
    }

    override fun finalizeWriting() {
        super.finalizeWriting()
        db.pagesQueries.createIndex()
    }
}
