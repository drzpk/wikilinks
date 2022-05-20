package dev.drzepka.wikilinks.generator.pipeline.sort

import com.google.common.io.CountingInputStream
import org.anarres.parallelgzip.ParallelGZIPOutputStream
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.floor

@Suppress("UnstableApiUsage")
class LinksFileSorter(private val file: File) {
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2)
    private val entries = ArrayList<Entry>(getLinksPerBlock())
    private val tmpFiles = ArrayList<File>()

    private val sourceFileSizeMB = file.length() / 1024 / 1024
    private val parentDirectory = file.parentFile
    private val tmpDirectory = File(parentDirectory, "tmpSort")
    private var totalLinks = 0L

    init {
        if (!tmpDirectory.isDirectory)
            tmpDirectory.mkdir()
        else
            tmpDirectory.listFiles()!!.forEach { it.delete() }
    }

    fun sort() {
        println("Sorting by source link")
        sort(0, "id_links_sorted_source.txt.gz")

        println("Sorting by target link")
        sort(1, "id_links_sorted_target.txt.gz")

        executor.shutdown()
    }

    private fun sort(sortColumn: Int, outputFileName: String) {
        createBlocks(sortColumn)

        val outFile = File(parentDirectory, outputFileName)
        if (outFile.isFile)
            outFile.delete()

        val outStream = FileOutputStream(outFile, false)
        val gzipOutStream = ParallelGZIPOutputStream(outStream, executor)
        val writer = BufferedWriter(OutputStreamWriter(gzipOutStream), FILE_BUFFER_SIZE)

        mergeBlocks(sortColumn, writer)
        writer.close()
    }

    private fun createBlocks(sortColumn: Int) {
        println("Creating blocks")

        val countingStream = CountingInputStream(FileInputStream(file))
        val gzipInStream = GZIPInputStream(countingStream, FILE_BUFFER_SIZE)
        val reader = BufferedReader(InputStreamReader(gzipInStream))
        val entriesPerBlock = getLinksPerBlock()

        var counter = 0L
        while (true) {
            val line = reader.readLine() ?: break
            entries.add(convertToEntry(sortColumn, line))
            totalLinks++

            if (++counter % 10000 == 0L)
                printBlocksProgress(countingStream.count)

            if (entries.size == entriesPerBlock)
                sortAndFlushEntries()
        }

        if (entries.isNotEmpty())
            sortAndFlushEntries()

        reader.close()
        println()
    }

    private fun printBlocksProgress(readBytes: Long) {
        val readMB = readBytes / 1024 / 1024
        val percentage = floor(readMB.toFloat() / sourceFileSizeMB * 1000) / 10
        print("\rProgress: $readMB/$sourceFileSizeMB MB   ($percentage%)    ")
    }

    private fun sortAndFlushEntries() {
        val tmpFile = File.createTempFile("sort-", null, tmpDirectory)
        tmpFile.deleteOnExit()
        tmpFiles.add(tmpFile)

        val stream = GZIPOutputStream(FileOutputStream(tmpFile), FILE_BUFFER_SIZE)
        val writer = BufferedWriter(OutputStreamWriter(stream))

        entries.sort()
        entries.forEach { writer.appendLine(it.line) }

        writer.close()
        entries.clear()
    }

    private fun convertToEntry(column: Int, line: String): Entry {
        val parts = line.split(",")
        return Entry(parts[column].toInt(), line)
    }

    private fun mergeBlocks(sortColumn: Int, writer: BufferedWriter) {
        println("Merging blocks")

        val queue = PriorityQueue<BlockFile>(tmpFiles.size)
        queue.addAll(tmpFiles.map { BlockFile(it, sortColumn) })

        var counter = 0L
        while (queue.isNotEmpty()) {
            val blockFile = queue.poll()
            writer.appendLine(blockFile.line)

            if (blockFile.next())
                queue.add(blockFile)
            else
                blockFile.close()

            if (++counter % 10_000 == 0L)
                printMergeProgress(counter)
        }
    }

    private fun printMergeProgress(count: Long) {
        val countThousands = count / 1000
        val totalThousands = totalLinks / 1000
        val percentage = floor(countThousands.toFloat() / totalThousands * 1000) / 10
        print("\rProgress: ${countThousands}k/${totalThousands}k   ($percentage%)    ")
    }

    // Total links (20220501): 963_013_717
    private fun getLinksPerBlock(): Int = 10_000_000

    companion object {
        private const val FILE_BUFFER_SIZE = 16 * 1024 * 1024
    }
}
