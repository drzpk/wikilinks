package dev.drzepka.wikilinks.generator.pipeline

import dev.drzepka.wikilinks.db.Database
import dev.drzepka.wikilinks.generator.model.Value
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PageWriter(private val db: Database) {
    private var activeBuffer = ArrayList<Value>()
    private var inactiveBuffer = ArrayList<Value>()
    private val writingLock = ReentrantLock()

    val pages = HashMap<Int, String>()

    fun write(value: Value) {
        // Only store pages with namespace == 0
        if (value[1] != 0)
            return

        activeBuffer.add(value)

        if (activeBuffer.size == BUFFER_SIZE)
            flush()
    }

    fun finalizeWriting() {
        flush()
        db.pagesQueries.createIndex()
    }

    private fun flush() {
        writingLock.withLock {
            val tmp = activeBuffer
            activeBuffer = inactiveBuffer
            inactiveBuffer = tmp
        }

        // This gap between unlocking the lock by this thread and locking it by the executor
        // is fine, because the PageWriter itself is not multithreaded.

        Thread(InsertExecutor(inactiveBuffer)).start()
    }

    private inner class InsertExecutor(private val buffer: MutableList<Value>) : Runnable {

        override fun run() {
            writingLock.withLock {
                db.transaction {
                    buffer.forEach { insert(it) }
                }

                buffer.clear()
            }
        }

        private fun insert(value: List<Any?>) {
            val id = value[0] as Int
            val title = value[2] as String
            val isRedirect = value[4] as Int

            db.pagesQueries.insert(id.toLong(), title, isRedirect.toLong())
            pages[id] = title
        }
    }

    companion object {
        private const val BUFFER_SIZE = 1_000_000
    }
}
