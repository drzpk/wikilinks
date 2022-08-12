package dev.drzepka.wikilinks.generator.pipeline.relocator

import dev.drzepka.wikilinks.generator.Configuration
import dev.drzepka.wikilinks.generator.flow.FlowRuntime
import dev.drzepka.wikilinks.generator.flow.FlowSegment
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.Store
import dev.drzepka.wikilinks.generator.pipeline.relocator.mover.FileMover
import dev.drzepka.wikilinks.generator.pipeline.relocator.mover.LocalFilesystemMover
import dev.drzepka.wikilinks.generator.pipeline.relocator.mover.S3Mover
import dev.drzepka.wikilinks.generator.utils.getFilePath
import org.anarres.parallelgzip.ParallelGZIPOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

class OutputRelocator(private val workingDirectory: File, private val outputUri: URI) : FlowSegment<Store> {
    override val numberOfSteps = (if (shouldCompress()) 1 else 0) + 1

    private val mover = createMover(Configuration.outputLocation)

    override fun run(store: Store, runtime: FlowRuntime) {
        val databaseFile = File(workingDirectory, store.linksDatabaseFile.fileName)
        var fileToMove = databaseFile

        if (shouldCompress()) {
            runtime.startNextStep("Compressing the output file")
            val key = "CompressionStep"
            if (store[key] == null) {
                fileToMove = compress(databaseFile, runtime)
                store[key] = "done"
            } else {
                println("Already compressed")
            }
        }

        runtime.startNextStep("Moving database file")
        val key = "MovingStep"
        if (store[key] == null) {
            val strippedUri = outputUri.toString().substringBefore("?")
            println("target: $strippedUri")
            mover.move(fileToMove, runtime)
            store[key] = "done"
        } else {
            println("Already moved")
        }
    }

    private fun createMover(rawUri: String): FileMover {
        val uri = URI.create(rawUri)
        return when (uri.scheme) {
            "file" -> {
                val file = uri.getFilePath()
                println("Output file will be saved in the local filesystem: $file")
                LocalFilesystemMover(file)
            }
            "s3" -> {
                val bucket = uri.host
                val directoryKey = uri.path
                println("Output file fill be saved in the S3 bucket $bucket under key $directoryKey")
                S3Mover(bucket, directoryKey)
            }
            else -> throw IllegalArgumentException("Unsupported output uri scheme: ${uri.scheme}")
        }
    }

    private fun shouldCompress(): Boolean = containsQueryParam("compress")

    @Suppress("SameParameterValue")
    private fun containsQueryParam(name: String): Boolean {
        val query = outputUri.query ?: ""
        return query.startsWith("$name=") || query.contains("&$name=")
    }

    private fun compress(inputFile: File, logger: ProgressLogger): File {
        val outputFile = File(inputFile.absolutePath + ".gz")
        val bufferedOutStream = BufferedOutputStream(FileOutputStream(outputFile), COMPRESSION_BUFFERS_SIZE)
        val gzipOutStream = ParallelGZIPOutputStream(bufferedOutStream)

        val inputStream = FileInputStream(inputFile)
        val buffer = ByteArray(COMPRESSION_BUFFERS_SIZE)

        val inputSizeMB = (inputFile.length() / 1024 / 1024).toInt()
        var readBytes = 0L
        var readIterations = 0

        while (true) {
            val len = inputStream.read(buffer)
            if (len == -1)
                break

            gzipOutStream.write(buffer, 0, len)

            readBytes += len
            if (++readIterations % 10 == 0) {
                val readMB = (readBytes / 1024 / 1024).toInt()
                logger.updateProgress(readMB, inputSizeMB, "MB")
            }
        }

        inputStream.close()
        gzipOutStream.close()
        inputFile.delete()

        return outputFile
    }

    companion object {
        private const val COMPRESSION_BUFFERS_SIZE = 8 * 1024 * 10248
    }
}
