package dev.drzepka.wikilinks.generator

import com.google.common.io.CountingInputStream
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.PageLinks
import dev.drzepka.wikilinks.generator.pipeline.link.LinksFileReader
import dev.drzepka.wikilinks.generator.pipeline.reader.GZipReader
import dev.drzepka.wikilinks.generator.pipeline.worker.WriterWorker
import dev.drzepka.wikilinks.generator.pipeline.writer.Writer
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ArrayBlockingQueue

@Suppress("UnstableApiUsage")
class LinksPipelineManager(
    private val writer: Writer<PageLinks>,
    sourceLinksFileName: String,
    targetLinksFileName: String
) {

    private var inLinksReader: LinksFileReader?
    private var outLinksReader: LinksFileReader?
    private val linkFilesSizeMB: Int

    private val inCountingStream: CountingInputStream
    private val outCountingStream: CountingInputStream

    private val pageLinksQueue = ArrayBlockingQueue<List<PageLinks>>(30)
    private lateinit var writerWorker: WriterWorker<PageLinks>

    init {
        // Source links = links sorted by a source link
        // Target links = links sorted by a target link
        val sourceLinksFile = File(sourceLinksFileName)
        val targetLinksFile = File(targetLinksFileName)
        linkFilesSizeMB = ((sourceLinksFile.length() + targetLinksFile.length()) / 1024 / 1024).toInt()

        inCountingStream = CountingInputStream(FileInputStream(targetLinksFile))
        outCountingStream = CountingInputStream(FileInputStream(sourceLinksFile))

        // In links = other pages linking to THIS page
        // Out links = THIS page linking to other pages
        inLinksReader = LinksFileReader(GZipReader(inCountingStream, 32 * 1024 * 1024), 1)
        outLinksReader = LinksFileReader(GZipReader(outCountingStream, 32 * 1024 * 1024), 0)
    }

    fun start(logger: ProgressLogger) {
        writerWorker = WriterWorker(pageLinksQueue, writer)
        Thread(writerWorker).apply {
            name = "lpm-writer-worker"
            start()
        }

        try {
            run(logger)
        } finally {
            writerWorker.stop()
        }
    }

    private fun run(logger: ProgressLogger) {
        var iterations = 0L
        while (true) {
            val links = getNextLinks() ?: break
            pageLinksQueue.put(listOf(links))

            if (++iterations % 100 == 0L) {
                val readMB = ((inCountingStream.count + outCountingStream.count) / 1024 / 1024).toInt()
                logger.updateProgress(readMB, linkFilesSizeMB, "MB")
            }
        }

        while (pageLinksQueue.isNotEmpty())
            Thread.sleep(200)
    }

    private fun getNextLinks(): PageLinks? {
        if (inLinksReader == null && outLinksReader == null)
            return null

        val groupingColumn = minOf(
            inLinksReader?.nextGroupingColumnValue ?: Int.MAX_VALUE,
            outLinksReader?.nextGroupingColumnValue ?: Int.MAX_VALUE
        )

        var inLinks = ""
        var outLinks = ""

        if (inLinksReader?.nextGroupingColumnValue == groupingColumn)
            inLinks = joinLinks(inLinksReader!!)
        if (outLinksReader?.nextGroupingColumnValue == groupingColumn)
            outLinks = joinLinks(outLinksReader!!)

        return PageLinks(groupingColumn, inLinks, outLinks)
    }

    private fun joinLinks(reader: LinksFileReader): String {
        val group = reader.next()
        val builder = StringBuilder(group.size * 8)
        val valueColumn = 1 - reader.groupingColumn

        for (i in 0 until group.size - 1) {
            builder.append(group[i][valueColumn])
            builder.append(',')
        }

        builder.append(group[group.lastIndex][valueColumn])
        return builder.toString()
    }
}
