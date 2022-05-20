package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.generator.model.Value
import org.anarres.parallelgzip.ParallelGZIPOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class LinksWriter(private val pages: Map<String, Int>) : Writer {
    private val executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2)
    private val linksFileWriter: java.io.Writer

    init {
        val linksFile = File("dumps/id_links.txt.gz")
        if (linksFile.isFile)
            linksFile.delete()

        val fileStream = BufferedOutputStream(FileOutputStream(linksFile, false), 32 * 1024 * 1024)
        val gzipStream = ParallelGZIPOutputStream(fileStream, executorService)
        linksFileWriter = OutputStreamWriter(gzipStream)
    }

    override fun write(value: Value) {
        val from = value[0] as Int
        val to = pages[value[2] as String] ?: return

        linksFileWriter.appendLine("$from,$to")
    }

    override fun finalizeWriting() {
        linksFileWriter.close()
        executorService.shutdown()
    }
}
