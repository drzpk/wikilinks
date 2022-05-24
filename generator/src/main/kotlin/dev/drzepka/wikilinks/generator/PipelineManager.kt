package dev.drzepka.wikilinks.generator

import com.google.common.io.CountingInputStream
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.reader.Reader
import dev.drzepka.wikilinks.generator.pipeline.worker.SqlWorker
import dev.drzepka.wikilinks.generator.pipeline.worker.WriterWorker
import dev.drzepka.wikilinks.generator.pipeline.writer.Writer
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ArrayBlockingQueue

@Suppress("UnstableApiUsage")
class PipelineManager(
    private val fileName: String,
    private val readerFactory: (stream: InputStream) -> Reader,
    private val writer: Writer,
    parallelismFactor: Float = 1.0f
) {
    private val fileSizeMB: Int
    private val sqlWorkerCount = (Runtime.getRuntime().availableProcessors() * parallelismFactor).toInt()
    private val statementQueue = ArrayBlockingQueue<String>(sqlWorkerCount * 3)
    private val valueQueue = ArrayBlockingQueue<List<Value>>(10)

    private val sqlWorkers = mutableListOf<SqlWorker>()
    private lateinit var writerWorker: WriterWorker
    private lateinit var logger: ProgressLogger

    init {
        val file = File(fileName)
        fileSizeMB = (file.length() / 1024 / 1024).toInt()
    }

    fun start(logger: ProgressLogger) {
        this.logger = logger

        startWorkers()
        readFile()
        waitForWorkers()
    }

    private fun startWorkers() {
        repeat(sqlWorkerCount) {
            val worker = SqlWorker(statementQueue, valueQueue)
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
        val countingStream = CountingInputStream(FileInputStream(fileName))
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
