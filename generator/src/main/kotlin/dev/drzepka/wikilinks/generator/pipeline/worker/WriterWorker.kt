package dev.drzepka.wikilinks.generator.pipeline.worker

import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.writer.AbstractWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.Writer
import java.util.concurrent.BlockingQueue

class WriterWorker(private val valueQueue: BlockingQueue<List<Value>>, private val writer: Writer) : Runnable {

    override fun run() {
        try {
            while (true) {
                val list = valueQueue.take()
                list.forEach { writer.write(it) }
            }
        } catch (e: InterruptedException) {
            writer.finalizeWriting()
        }
    }
}
