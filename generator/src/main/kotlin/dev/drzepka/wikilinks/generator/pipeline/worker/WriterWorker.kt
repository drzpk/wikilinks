package dev.drzepka.wikilinks.generator.pipeline.worker

import dev.drzepka.wikilinks.generator.pipeline.writer.Writer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WriterWorker<T>(private val valueQueue: BlockingQueue<List<T>>, private val writer: Writer<T>) : Runnable {
    private val working = AtomicBoolean(true)

    override fun run() {
        while (working.get()) {
            valueQueue.poll(1, TimeUnit.SECONDS)?.forEach { writer.write(it) }
        }

        writer.finalizeWriting()
    }

    fun stop() {
        working.set(false)
    }
}
