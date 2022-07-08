package dev.drzepka.wikilinks.generator

import com.google.common.io.CountingInputStream
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.LinkGroup
import dev.drzepka.wikilinks.generator.model.PageLinks
import dev.drzepka.wikilinks.generator.pipeline.reader.GZipReader
import dev.drzepka.wikilinks.generator.pipeline.reader.LinksFileReader
import dev.drzepka.wikilinks.generator.pipeline.worker.WriterWorker
import dev.drzepka.wikilinks.generator.pipeline.writer.Writer
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

@Suppress("UnstableApiUsage")
class LinksPipelineManager(
    private val writer: Writer<PageLinks>,
    sourceLinksFile: File,
    targetLinksFile: File
) {
    private var inLinksQueueWrapper: BufferingQueueWrapper<LinkGroup>?
    private var outLinksQueueWrapper: BufferingQueueWrapper<LinkGroup>?
    private val linksWriteQueue = ArrayBlockingQueue<List<PageLinks>>(30)

    private var inLinksReader: LinksFileReader
    private var outLinksReader: LinksFileReader

    private val linkFilesSizeMB: Int
    private val inCountingStream: CountingInputStream
    private val outCountingStream: CountingInputStream

    init {
        // Source links = links sorted by a source link
        // Target links = links sorted by a target link
        linkFilesSizeMB = ((sourceLinksFile.length() + targetLinksFile.length()) / 1024 / 1024).toInt()

        inCountingStream = CountingInputStream(FileInputStream(targetLinksFile))
        outCountingStream = CountingInputStream(FileInputStream(sourceLinksFile))

        val inLinksQueue: BlockingQueue<LinkGroup> = ArrayBlockingQueue(100)
        val outLinksQueue: BlockingQueue<LinkGroup> = ArrayBlockingQueue(100)

        // In links = other pages linking to THIS page
        // Out links = THIS page linking to other pages
        inLinksReader = LinksFileReader(GZipReader(inCountingStream, 16 * 1024 * 1024), inLinksQueue, 1)
        outLinksReader = LinksFileReader(GZipReader(outCountingStream, 16 * 1024 * 1024), outLinksQueue, 0)

        inLinksQueueWrapper = BufferingQueueWrapper(inLinksQueue, inLinksReader)
        outLinksQueueWrapper = BufferingQueueWrapper(outLinksQueue, outLinksReader)
    }

    fun start(logger: ProgressLogger) {
        val inReaderThread = Thread(inLinksReader, "in-links-reader").apply { start() }
        val outReaderThread = Thread(outLinksReader, "out-links-reader").apply { start() }

        val writerWorker = WriterWorker(linksWriteQueue, writer)
        val writerWorkerThread = Thread(writerWorker, "lpm-writer-worker").apply { start() }

        try {
            run(logger)
        } finally {
            inReaderThread.join(1_000)
            if (inReaderThread.isAlive)
                inReaderThread.interrupt()

            outReaderThread.join(1_000)
            if (outReaderThread.isAlive)
                outReaderThread.interrupt()

            writerWorker.stop()
            writerWorkerThread.join()
        }
    }

    private fun run(logger: ProgressLogger) {
        var iterations = 0L
        while (true) {
            val links = getNextLinks() ?: break
            linksWriteQueue.put(listOf(links))

            if (++iterations % 100 == 0L) {
                val readMB = ((inCountingStream.count + outCountingStream.count) / 1024 / 1024).toInt()
                logger.updateProgress(readMB, linkFilesSizeMB, "MB")
            }
        }

        while (linksWriteQueue.isNotEmpty())
            Thread.sleep(200)
    }

    private fun getNextLinks(): PageLinks? {
        val inGroup = inLinksQueueWrapper?.peek()
        val outGroup = outLinksQueueWrapper?.peek()

        if (inGroup == null && outGroup == null)
            return null

        val minGroupingValue = minOf(
            inGroup?.groupingValue ?: Int.MAX_VALUE,
            outGroup?.groupingValue ?: Int.MAX_VALUE
        )

        var inLinks = ""
        var outLinks = ""
        var inLinksCount = 0
        var outLinksCount = 0

        if (inGroup?.groupingValue == minGroupingValue) {
            inLinks = joinLinks(inGroup, inLinksReader.groupingColumn)
            inLinksCount = inGroup.links.size
            inLinksQueueWrapper?.consume()

            if (inLinksReader.done && inLinksQueueWrapper?.isEmpty() == true)
                inLinksQueueWrapper = null
        }

        if (outGroup?.groupingValue == minGroupingValue) {
            outLinks = joinLinks(outGroup, outLinksReader.groupingColumn)
            outLinksCount = outGroup.links.size
            outLinksQueueWrapper?.consume()

            if (outLinksReader.done && outLinksQueueWrapper?.isEmpty() == true)
                outLinksQueueWrapper = null
        }

        return PageLinks(minGroupingValue, inLinksCount, outLinksCount, inLinks, outLinks)
    }

    private fun joinLinks(group: LinkGroup, groupingColumn: Int): String {
        val links = group.links

        val builder = StringBuilder(links.size * 8)
        val valueColumn = 1 - groupingColumn

        for (i in 0 until links.size - 1) {
            builder.append(links[i][valueColumn])
            builder.append(',')
        }

        builder.append(links[links.lastIndex][valueColumn])
        return builder.toString()
    }

    private class BufferingQueueWrapper<T>(private val queue: BlockingQueue<T>, private val reader: LinksFileReader) {
        private var buffer: T? = null

        fun peek(): T? {
            if (buffer == null) {
                while (!reader.done && buffer == null)
                    buffer = queue.poll(1, TimeUnit.SECONDS)
            }

            return buffer
        }

        fun consume() {
            buffer = null
        }

        fun isEmpty(): Boolean = queue.isEmpty()
    }
}
