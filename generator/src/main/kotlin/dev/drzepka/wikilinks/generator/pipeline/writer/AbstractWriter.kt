package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.db.Database
import dev.drzepka.wikilinks.generator.model.Value
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class AbstractWriter(protected val db: Database) {
    private var activeBuffer = ArrayList<Value>()
    private var inactiveBuffer = ArrayList<Value>()
    private val writingLock = ReentrantLock()

    fun write(value: Value) {
        // Only store pages with namespace == 0
        if (value[1] != 0)
            return

        activeBuffer.add(value)

        if (activeBuffer.size == BUFFER_SIZE)
            flush()
    }

    open fun finalizeWriting() {
        flush()
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

    abstract fun insert(value: List<Any?>)

    private inner class InsertExecutor(private val buffer: MutableList<Value>) : Runnable {

        override fun run() {
            writingLock.withLock {
                db.transaction {
                    buffer.forEach { insert(it) }
                }

                buffer.clear()
            }
        }
    }

    companion object {
        private const val BUFFER_SIZE = 1_000_000
    }
}
