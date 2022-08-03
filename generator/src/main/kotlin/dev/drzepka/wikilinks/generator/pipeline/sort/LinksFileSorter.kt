package dev.drzepka.wikilinks.generator.pipeline.sort

import com.google.common.io.CountingInputStream
import dev.drzepka.wikilinks.generator.availableHeap
import dev.drzepka.wikilinks.generator.availableProcessors
import dev.drzepka.wikilinks.generator.flow.FlowRuntime
import dev.drzepka.wikilinks.generator.flow.FlowSegment
import dev.drzepka.wikilinks.generator.flow.FlowStorage
import dev.drzepka.wikilinks.generator.model.Store
import org.anarres.parallelgzip.ParallelGZIPOutputStream
import java.io.*
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream
import kotlin.math.floor

@Suppress("UnstableApiUsage")
class LinksFileSorter(private val file: File) : FlowSegment<Store> {
    private val executor = Executors.newFixedThreadPool((availableProcessors() / 2).coerceAtLeast(1))
    private val blockFileSorters = mutableListOf<BlockFileSorter>()

    private val sourceFileSizeMB by lazy { (file.length() / 1024 / 1024).toInt() }
    private val parentDirectory = file.parentFile
    private var totalLinks = 0L

    override val numberOfSteps = 4
    private lateinit var runtime: FlowRuntime

    override fun run(store: Store, runtime: FlowRuntime) {
        if (executor.isShutdown)
            throw IllegalStateException("Instance cannot be reused")

        this.runtime = runtime

        try {
            sort(0, SORTED_SOURCE_FILE_NAME, "source", store)
            sort(1, SORTED_TARGET_FILE_NAME, "target", store)
        } finally {
            executor.shutdown()
        }
    }

    private fun sort(sortColumn: Int, outputFileName: String, progressDescription: String, storage: FlowStorage) {
        blockFileSorters.clear()
        totalLinks = 0L

        val run = if (canSkipSort(outputFileName, progressDescription, storage)) {
            println("Links file is already sorted by $progressDescription, skipping")
            false
        } else true

        this.runtime.startNextStep("Creating blocks sorted by $progressDescription link")
        if (run)
            createBlocks(sortColumn)

        this.runtime.startNextStep("Merging blocks ($progressDescription)")
        if (run)
            mergeBlocks(sortColumn, outputFileName)

        storage[getProgressSaveKey(progressDescription)] = "complete"
    }

    private fun canSkipSort(outputFileName: String, progressDescription: String, storage: FlowStorage): Boolean {
        if (storage[getProgressSaveKey(progressDescription)] == null)
            return false

        val outputFileExists = File(parentDirectory, outputFileName).isFile
        if (!outputFileExists)
            println("WARN: progress for $progressDescription was saved, but no output file exists")

        return outputFileExists
    }

    private fun createBlocks(sortColumn: Int) {
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
                runtime.updateProgress((countingStream.count / 1024 / 1024).toInt(), sourceFileSizeMB, "MB")

            if (linkBuffer.size == linksPerBlock) {
                val oldBuffer = linkBuffer
                linkBuffer = ArrayList(linksPerBlock)
                blockSortQueue.put(oldBuffer)
            }
        }

        reader.close()

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

    private fun mergeBlocks(sortColumn: Int, outputFileName: String) {
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
                runtime.updateProgress((counter / 1000).toInt(), (totalLinks / 1000).toInt(), "k")
        }

        writer.close()
    }

    private fun createSortedFileWriter(outputFileName: String): BufferedWriter {
        val outFile = File(parentDirectory, outputFileName)
        if (outFile.isFile)
            outFile.delete()

        val outStream = FileOutputStream(outFile, false)
        val gzipOutStream = ParallelGZIPOutputStream(outStream, executor)
        return BufferedWriter(OutputStreamWriter(gzipOutStream), FILE_BUFFER_SIZE)
    }

    // Total links (20220501): 963_013_717
    private fun getLinksPerBlock(): Int {
        // Based on tests with a profiler
        val linksPerGBHeap = 5_000_000 / 6.0
        val maxHeapBytes = availableHeap()
        val maxHeapGB = maxHeapBytes.toDouble() / 1024 / 1024 / 1024
        return floor(linksPerGBHeap * maxHeapGB + 0.5).toInt()
    }

    private fun getBlockSortingThreads(): Int = 2

    private fun getProgressSaveKey(progressDescription: String): String = "links_file_sorter_$progressDescription"

    companion object {
        private const val FILE_BUFFER_SIZE = 16 * 1024 * 1024
        const val SORTED_SOURCE_FILE_NAME = "id_links_sorted_source.txt.gz"
        const val SORTED_TARGET_FILE_NAME = "id_links_sorted_target.txt.gz"
    }
}
