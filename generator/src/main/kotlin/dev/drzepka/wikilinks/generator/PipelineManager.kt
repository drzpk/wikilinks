package dev.drzepka.wikilinks.generator

import com.google.common.io.CountingInputStream
import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.PageWriter
import dev.drzepka.wikilinks.generator.pipeline.SqlDumpReader
import dev.drzepka.wikilinks.generator.pipeline.worker.SqlWorker
import dev.drzepka.wikilinks.generator.pipeline.worker.WriterWorker
import java.io.File
import java.io.FileInputStream
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.floor

@Suppress("UnstableApiUsage")
class PipelineManager(
    private val fileName: String,
    private val description: String,
    private val writer: PageWriter
) {
    private val fileSizeMB: Int
    private val sqlWorkerCount = Runtime.getRuntime().availableProcessors()
    private val statementQueue = ArrayBlockingQueue<String>(sqlWorkerCount * 3)
    private val valueQueue = ArrayBlockingQueue<List<Value>>(10)

    private val sqlWorkerThreads = mutableListOf<Thread>()
    private lateinit var writerWorkerThread: Thread

    init {
        val file = File(fileName)
        fileSizeMB = (file.length() / 1024 / 1024).toInt()
    }

    fun start() {
        println("Starting pipeline: $description")
        startWorkers()

        val startTime = Instant.now()
        readFile()
        waitForWorkers()

        val endTime = Instant.now()
        val duration = Duration.between(startTime, endTime).seconds
        println("\nPipeline has finished. The process took $duration seconds")

        destroyWorkers()
    }

    private fun startWorkers() {
        repeat(sqlWorkerCount) {
            val thread = Thread(SqlWorker(statementQueue, valueQueue))
            thread.name = "sql-worker-$it"
            thread.start()
            sqlWorkerThreads.add(thread)
        }

        val writerWorker = WriterWorker(valueQueue, writer)
        writerWorkerThread = Thread(writerWorker).apply {
            name = "writer-worker"
            start()
        }
    }

    private fun waitForWorkers() {
        while (statementQueue.isNotEmpty())
            Thread.sleep(200)

        while (valueQueue.isNotEmpty())
            Thread.sleep(200)
    }

    private fun destroyWorkers() {
        sqlWorkerThreads.forEach { it.interrupt() }
        writerWorkerThread.interrupt()
    }

    private fun readFile() {
        val countingStream = CountingInputStream(FileInputStream(fileName))
        val reader = SqlDumpReader(countingStream)
        var readStatements = 0

        while (reader.hasNext()) {
            val next = reader.next()
            statementQueue.put(next)

            if (readStatements++ % 10 == 0)
                printProgress(countingStream.count)
        }

        reader.close()
    }

    private fun printProgress(readBytes: Long) {
        val readMB = readBytes / 1024 / 1024
        val percentage = floor(readMB.toFloat() / fileSizeMB * 1000) / 10
        print("\rProgress: $readMB/$fileSizeMB MB   ($percentage%)    ")
    }
}
