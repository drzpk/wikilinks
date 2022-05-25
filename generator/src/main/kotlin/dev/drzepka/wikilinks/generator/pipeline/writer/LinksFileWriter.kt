package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.generator.model.Value
import org.anarres.parallelgzip.ParallelGZIPOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class LinksFileWriter(private val pages: Map<String, Int>) : Writer<Value> {
    private val executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2)
    private val fileWriter: java.io.Writer

    init {
        val linksFile = File("dumps/id_links.txt.gz")
        if (linksFile.isFile)
            linksFile.delete()

        val fileStream = BufferedOutputStream(FileOutputStream(linksFile, false), 32 * 1024 * 1024)
        val gzipStream = ParallelGZIPOutputStream(fileStream, executorService)
        fileWriter = OutputStreamWriter(gzipStream)
    }

    override fun write(value: Value) {
        // Check the namespace
        if (value[1] != 0)
            return

        val from = value[0] as Int
        val to = pages[value[2] as String] ?: return

        fileWriter.appendLine("$from,$to")
    }

    override fun finalizeWriting() {
        fileWriter.close()
        executorService.shutdown()
    }
}
