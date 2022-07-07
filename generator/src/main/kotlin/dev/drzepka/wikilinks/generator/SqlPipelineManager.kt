package dev.drzepka.wikilinks.generator

import com.google.common.io.CountingInputStream
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.processor.Processor
import dev.drzepka.wikilinks.generator.pipeline.reader.Reader
import dev.drzepka.wikilinks.generator.pipeline.worker.SqlWorker
import dev.drzepka.wikilinks.generator.pipeline.worker.WriterWorker
import dev.drzepka.wikilinks.generator.pipeline.writer.Writer
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ArrayBlockingQueue

@Suppress("UnstableApiUsage")
class SqlPipelineManager(
    private val dumpFile: File,
    private val readerFactory: (stream: InputStream) -> Reader,
    private val writer: Writer<Value>,
    private val valueProcessor: Processor<Value>? = null,
    parallelismFactor: Float = 1.0f
) {
    private val fileSizeMB: Int = (dumpFile.length() / 1024 / 1024).toInt()
    private val sqlWorkerCount = (availableProcessors() * parallelismFactor).toInt().coerceAtLeast(1)
    private val statementQueue = ArrayBlockingQueue<String>(sqlWorkerCount * 3)
    private val valueQueue = ArrayBlockingQueue<List<Value>>(10)

    private val sqlWorkers = mutableListOf<SqlWorker>()
    private lateinit var writerWorker: WriterWorker<Value>
    private lateinit var logger: ProgressLogger

    fun start(logger: ProgressLogger) {
        this.logger = logger

        startWorkers()
        readFile()
        waitForWorkers()
    }

    private fun startWorkers() {
        repeat(sqlWorkerCount) {
            val worker = SqlWorker(statementQueue, valueQueue, valueProcessor)
            sqlWorkers.add(worker)

            val thread = Thread(worker)
            thread.name = "sql-worker-$it"
            thread.start()
        }

        writerWorker = WriterWorker(valueQueue, writer)
        Thread(writerWorker).apply {
            name = "writer-worker"
            start()
        }
    }

    private fun waitForWorkers() {
        while (statementQueue.isNotEmpty())
            Thread.sleep(200)

        sqlWorkers.forEach { it.stop() }
        writerWorker.stop()
    }

    private fun readFile() {
        val countingStream = CountingInputStream(FileInputStream(dumpFile))
        val reader = readerFactory.invoke(countingStream)
        var readStatements = 0

        while (reader.hasNext()) {
            val next = reader.next()
            statementQueue.put(next)

            if (readStatements++ % 10 == 0)
                logger.updateProgress((countingStream.count / 1024 / 1024).toInt(), fileSizeMB, "MB")
        }

        reader.close()
    }
}
