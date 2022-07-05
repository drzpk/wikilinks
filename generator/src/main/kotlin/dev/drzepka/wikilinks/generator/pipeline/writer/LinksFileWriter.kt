package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.generator.model.Value
import org.anarres.parallelgzip.ParallelGZIPOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class LinksFileWriter(workingDirectory: File) : Writer<Value> {
    private val executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2)
    private val fileWriter: java.io.Writer

    init {
        val linksFile = File(workingDirectory, LINKS_FILE_NAME)
        if (linksFile.isFile)
            linksFile.delete()

        val fileStream = BufferedOutputStream(FileOutputStream(linksFile, false), 32 * 1024 * 1024)
        val gzipStream = ParallelGZIPOutputStream(fileStream, executorService)
        fileWriter = OutputStreamWriter(gzipStream)
    }

    override fun write(value: Value) {
        val from = value[0] as Int
        val to = value[1] as Int
        fileWriter.appendLine("$from,$to")
    }

    override fun finalizeWriting() {
        fileWriter.close()
        executorService.shutdown()
    }

    companion object {
        const val LINKS_FILE_NAME = "id_links.txt.gz"
    }
}
