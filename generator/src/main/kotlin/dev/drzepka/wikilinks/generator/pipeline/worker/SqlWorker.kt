package dev.drzepka.wikilinks.generator.pipeline.worker

import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.SqlValueExtractor
import dev.drzepka.wikilinks.generator.pipeline.ValueParser
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SqlWorker(
    private val insertStatementQueue: BlockingQueue<String>,
    private val valueQueue: BlockingQueue<List<Value>>
) : Runnable {

    private val extractor = SqlValueExtractor()
    private val parser = ValueParser()
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
        val values = extractor
            .extractFromStatement(stmt)
            .map {
                parser.parse(it)
            }

        valueQueue.put(values)
    }
}
