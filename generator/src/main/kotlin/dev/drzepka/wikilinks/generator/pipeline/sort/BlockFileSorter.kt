package dev.drzepka.wikilinks.generator.pipeline.sort

import org.anarres.parallelgzip.ParallelGZIPOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BlockFileSorter(
    private val sortColumn: Int,
    private val inputQueue: BlockingQueue<ArrayList<String>>,
    private val gzipExecutor: ExecutorService,
    private val workingDirectory: File
) : Runnable {

    private val working = AtomicBoolean(true)
    private val files = mutableListOf<File>()

    fun stop() {
        working.set(false)
    }

    fun getFiles(): List<File> = files.toList()

    override fun run() {
        while (working.get()) {
            val block = inputQueue.poll(1, TimeUnit.SECONDS)
            if (block != null)
                processBlock(block)
        }
    }

    private fun processBlock(list: ArrayList<String>) {
        val entries = ArrayList<Entry>(list.size)
        list.mapTo(entries) { convertToEntry(it) }

        list.clear()
        list.trimToSize()

        entries.sort()
        flushToFile(entries)
    }

    private fun flushToFile(entries: List<Entry>) {
        val file = File.createTempFile("sort-", null, workingDirectory)
        file.deleteOnExit()
        files.add(file)

        val stream = ParallelGZIPOutputStream(FileOutputStream(file), gzipExecutor)
        val writer = BufferedWriter(OutputStreamWriter(stream), 32 * 1024 * 1024)

        entries.forEach { writer.appendLine(it.line) }
        writer.close()
    }

    private fun convertToEntry(line: String): Entry {
        val parts = line.split(",")
        return Entry(parts[sortColumn].toInt(), line)
    }
}
