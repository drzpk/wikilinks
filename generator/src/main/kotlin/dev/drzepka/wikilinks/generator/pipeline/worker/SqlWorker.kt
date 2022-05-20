package dev.drzepka.wikilinks.generator.pipeline.worker

import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.SqlValueExtractor
import dev.drzepka.wikilinks.generator.pipeline.ValueParser
import java.util.concurrent.BlockingQueue

class SqlWorker(
    private val insertStatementQueue: BlockingQueue<String>,
    private val valueQueue: BlockingQueue<List<Value>>
) : Runnable {

    private val extractor = SqlValueExtractor()
    private val parser = ValueParser()

    override fun run() {
        try {
            while (true) {
                val statement = insertStatementQueue.take()
                processStatement(statement)
            }
        } catch (ignored: InterruptedException) {
            // All done
        }
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
                if (it.startsWith("40435962,0,'Bhongir_(Lok_Sabha_constituency"))
                    println("gotcha")
                parser.parse(it)
            }

        valueQueue.put(values)
    }
}
