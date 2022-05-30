package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.db.Database
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class AbstractWriter<T>(protected val db: Database, private val bufferSize: Int) : Writer<T> {
    private var activeBuffer = ArrayList<T>()
    private var inactiveBuffer = ArrayList<T>()
    private val writingLock = ReentrantLock()
    private val isFlushing = AtomicBoolean(false)

    override fun write(value: T) {
        if (!filter(value))
            return

        activeBuffer.add(value)

        if (activeBuffer.size == bufferSize)
            flush()
    }

    open fun filter(value: T): Boolean = true

    override fun finalizeWriting() {
        flush()
        while (isFlushing.get())
            Thread.sleep(250)
    }

    private fun flush() {
        writingLock.withLock {
            val tmp = activeBuffer
            activeBuffer = inactiveBuffer
            inactiveBuffer = tmp
        }

        // This gap between unlocking the lock by this thread and locking it by the executor
        // is fine, because the PageWriter itself is not multithreaded.

        isFlushing.set(true)
        Thread(InsertExecutor(inactiveBuffer)).start()
    }

    abstract fun insert(value: T)

    open fun onInsertComplete() = Unit

    private inner class InsertExecutor(private val buffer: MutableList<T>) : Runnable {

        override fun run() {
            writingLock.withLock {
                db.transaction {
                    buffer.forEach { insert(it) }
                    onInsertComplete()
                }

                buffer.clear()
                isFlushing.set(false)
            }
        }
    }
}
