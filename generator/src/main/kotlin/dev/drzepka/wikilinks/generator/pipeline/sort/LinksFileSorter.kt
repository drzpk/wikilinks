package dev.drzepka.wikilinks.generator.pipeline.sort

import com.google.common.io.CountingInputStream
import org.anarres.parallelgzip.ParallelGZIPOutputStream
import java.io.*
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream
import kotlin.math.floor

@Suppress("UnstableApiUsage")
class LinksFileSorter(private val file: File) {
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2)
    private val blockFileSorters = mutableListOf<BlockFileSorter>()

    private val sourceFileSizeMB = file.length() / 1024 / 1024
    private val parentDirectory = file.parentFile
    private var totalLinks = 0L

    fun sort() {
        if (executor.isShutdown)
            throw IllegalStateException("Instance cannot be reused")

        try {
            println("Sorting by source link")
            sort(0, "id_links_sorted_source.txt.gz")

            println("Sorting by target link")
            sort(1, "id_links_sorted_target.txt.gz")
        } finally {
            executor.shutdown()
        }
    }

    private fun sort(sortColumn: Int, outputFileName: String) {
        blockFileSorters.clear()
        totalLinks = 0L

        createBlocks(sortColumn)
        mergeBlocks(sortColumn, outputFileName)
    }

    private fun createBlocks(sortColumn: Int) {
        println("Creating blocks")

        val countingStream = CountingInputStream(FileInputStream(file))
        val gzipInStream = GZIPInputStream(countingStream, FILE_BUFFER_SIZE)
        val reader = BufferedReader(InputStreamReader(gzipInStream))
        val linksPerBlock = getLinksPerBlock()

        val blockSortQueue = ArrayBlockingQueue<ArrayList<String>>(getBlockSortingThreads() + 1)
        val sorterThreads = createSorterThreads(sortColumn, blockSortQueue)

        var counter = 0L
        var linkBuffer = ArrayList<String>(linksPerBlock)

        while (true) {
            val line = reader.readLine() ?: break
            linkBuffer.add(line)
            totalLinks++

            if (++counter % 10000 == 0L)
                printBlocksProgress(countingStream.count)

            if (linkBuffer.size == linksPerBlock) {
                val oldBuffer = linkBuffer
                linkBuffer = ArrayList(linksPerBlock)
                blockSortQueue.put(oldBuffer)
            }
        }

        reader.close()
        println()

        if (linkBuffer.isNotEmpty())
            blockSortQueue.put(linkBuffer)

        while (blockSortQueue.isNotEmpty())
            Thread.sleep(200)

        blockFileSorters.forEach { it.stop() }
        sorterThreads.forEach { it.join() }
    }

    private fun createSorterThreads(sortColumn: Int, blockSortQueue: BlockingQueue<ArrayList<String>>): List<Thread> {
        val tmpSortDirectory = File(parentDirectory, "tmpSort")

        if (!tmpSortDirectory.isDirectory)
            tmpSortDirectory.mkdir()
        else
            tmpSortDirectory.listFiles()!!.forEach { it.delete() }

        val sorterThreads = (0 until getBlockSortingThreads()).map {
            val sorter = BlockFileSorter(sortColumn, blockSortQueue, executor, tmpSortDirectory)
            blockFileSorters.add(sorter)
            Thread(sorter).apply { start() }
        }

        return sorterThreads
    }

    private fun printBlocksProgress(readBytes: Long) {
        val readMB = readBytes / 1024 / 1024
        val percentage = floor(readMB.toFloat() / sourceFileSizeMB * 1000) / 10
        print("\rProgress: $readMB/$sourceFileSizeMB MB   ($percentage%)    ")
    }

    private fun mergeBlocks(sortColumn: Int, outputFileName: String) {
        println("Merging blocks")

        val tmpFiles = blockFileSorters.flatMap { it.getFiles() }
        val writer = createSortedFileWriter(outputFileName)
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

        writer.close()
        println()
    }

    private fun createSortedFileWriter(outputFileName: String): BufferedWriter {
        val outFile = File(parentDirectory, outputFileName)
        if (outFile.isFile)
            outFile.delete()

        val outStream = FileOutputStream(outFile, false)
        val gzipOutStream = ParallelGZIPOutputStream(outStream, executor)
        return BufferedWriter(OutputStreamWriter(gzipOutStream), FILE_BUFFER_SIZE)
    }

    private fun printMergeProgress(count: Long) {
        val countThousands = count / 1000
        val totalThousands = totalLinks / 1000
        val percentage = floor(countThousands.toFloat() / totalThousands * 1000) / 10
        print("\rProgress: ${countThousands}k/${totalThousands}k   ($percentage%)    ")
    }

    // Total links (20220501): 963_013_717
    private fun getLinksPerBlock(): Int = 10_000_000

    private fun getBlockSortingThreads(): Int = 2

    companion object {
        private const val FILE_BUFFER_SIZE = 16 * 1024 * 1024
    }
}
