package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.db.Database
import dev.drzepka.wikilinks.generator.model.Value
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class AbstractWriter(protected val db: Database, private val bufferSize: Int) : Writer {
    private var activeBuffer = ArrayList<Value>()
    private var inactiveBuffer = ArrayList<Value>()
    private val writingLock = ReentrantLock()

    override fun write(value: Value) {
        if (!filter(value))
            return

        activeBuffer.add(value)

        if (activeBuffer.size == bufferSize)
            flush()
    }

    open fun filter(value: Value): Boolean = true

    override fun finalizeWriting() {
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

    open fun onInsertComplete() = Unit

    private inner class InsertExecutor(private val buffer: MutableList<Value>) : Runnable {

        override fun run() {
            writingLock.withLock {
                db.transaction {
                    buffer.forEach { insert(it) }
                    onInsertComplete()
                }

                buffer.clear()
            }
        }
    }
}
