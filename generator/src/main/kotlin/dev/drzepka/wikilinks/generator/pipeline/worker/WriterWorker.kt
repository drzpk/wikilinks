package dev.drzepka.wikilinks.generator.pipeline.worker

import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.writer.Writer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WriterWorker(private val valueQueue: BlockingQueue<List<Value>>, private val writer: Writer) : Runnable {
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
