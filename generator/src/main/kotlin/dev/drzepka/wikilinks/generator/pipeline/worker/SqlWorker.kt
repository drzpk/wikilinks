package dev.drzepka.wikilinks.generator.pipeline.worker

import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.SqlStatementParser
import dev.drzepka.wikilinks.generator.pipeline.processor.Processor
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SqlWorker(
    private val insertStatementQueue: BlockingQueue<String>,
    private val valueQueue: BlockingQueue<List<Value>>,
    private val valueProcessor: Processor<Value>?
) : Runnable {
    private val working = AtomicBoolean(true)

    override fun run() {
        while (working.get()) {
            val statement = insertStatementQueue.poll(1, TimeUnit.SECONDS)
            if (statement != null)
                processStatement(statement)
        }
    }

    fun stop() {
        working.set(false)
    }

    private fun processStatement(stmt: String) {
        try {
            doProcessStatement(stmt)
        } catch (e: Exception) {
            println("Error while processing statement")
            e.printStackTrace()
        }
    }

    private fun doProcessStatement(stmt: String) {
        var sequence = SqlStatementParser(stmt).asSequence()
        if (valueProcessor != null)
            sequence = sequence.mapNotNull { valueProcessor.process(it) }

        valueQueue.put(sequence.toList())
    }
}
