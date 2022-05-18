package dev.drzepka.wikilinks.generator.pipeline

import dev.drzepka.wikilinks.db.Database

class PageWriter(private val db: Database) {
    private val buffer = ArrayList<List<Any?>>()

    val pages = HashMap<Int, String>()

    fun write(value: List<Any?>) {
        // Only store pages with namespace == 0
        if (value[1] != 0)
            return

        buffer.add(value)

        if (buffer.size == BUFFER_SIZE) {
            flush()
            buffer.clear()
        }
    }

    fun flush() {
        db.transaction {
            buffer.forEach { insert(it) }
        }
    }

    private fun insert(value: List<Any?>) {
        val id = value[0] as Int
        val title = value[2] as String
        val isRedirect = value[4] as Int

        db.pagesQueries.insert(id.toLong(), title, isRedirect.toLong())
        pages[id] = title
    }

    companion object {
        private const val BUFFER_SIZE = 1_000_000
    }
}