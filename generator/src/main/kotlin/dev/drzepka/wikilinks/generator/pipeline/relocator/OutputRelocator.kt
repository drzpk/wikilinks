package dev.drzepka.wikilinks.generator.pipeline.relocator

import dev.drzepka.wikilinks.generator.flow.FlowRuntime
import dev.drzepka.wikilinks.generator.flow.FlowSegment
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.Store
import dev.drzepka.wikilinks.generator.pipeline.relocator.mover.FileMover
import dev.drzepka.wikilinks.generator.pipeline.relocator.mover.LocalFilesystemMover
import dev.drzepka.wikilinks.generator.pipeline.relocator.mover.S3Mover
import dev.drzepka.wikilinks.generator.utils.isQueryParamTrue
import dev.drzepka.wikilinks.generator.utils.getFilePath
import dev.drzepka.wikilinks.generator.utils.getQueryParamValue
import org.anarres.parallelgzip.ParallelGZIPOutputStream
import org.apache.http.client.utils.URIBuilder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

class OutputRelocator(private val workingDirectory: File, outputUri: URI, version: String) :
    FlowSegment<Store> {
    override val numberOfSteps = (if (shouldCompress(outputUri)) 1 else 0) + 1

    private val outputUri: URI
    private val mover: FileMover

    init {
        this.outputUri = if (shouldIncludeVersionInPath(outputUri)) {
            val builder = URIBuilder(outputUri)
            val pathSegments = builder.pathSegments.toMutableList()
            pathSegments.add(version)
            builder.pathSegments = pathSegments
            builder.build()
        } else outputUri

        mover = createMover(this.outputUri)
    }

    override fun run(store: Store, runtime: FlowRuntime) {
        val databaseFile = File(workingDirectory, store.linksDatabaseFile.fileName)
        var fileToMove = databaseFile

        if (shouldCompress(outputUri)) {
            runtime.startNextStep("Compressing the output file")
            val key = "CompressionStep"
            fileToMove = File(databaseFile.absolutePath + ".gz")
            if (store[key] == null) {
                fileToMove = compress(databaseFile, fileToMove, runtime)
                store[key] = "done"
            } else {
                println("Already compressed")
            }
        }

        runtime.startNextStep("Moving database file")
        val key = "MovingStep"
        if (store[key] == null) {
            val strippedUri = outputUri.toString().substringBefore("?")
            println("target directory: $strippedUri")
            mover.move(fileToMove, runtime)
            store[key] = "done"
        } else {
            println("Already moved")
        }
    }

    private fun createMover(outputUri: URI): FileMover {
        return when (outputUri.scheme) {
            "file" -> {
                val file = outputUri.getFilePath()
                println("Output file will be saved in the local filesystem: $file")
                LocalFilesystemMover(file)
            }
            "s3" -> {
                val bucket = outputUri.host
                val directoryKey = outputUri.path
                println("Output file fill be saved in the S3 bucket $bucket under key $directoryKey")
                S3Mover(bucket, directoryKey, outputUri.getQueryParamValue("endpoint-host"))
            }
            else -> throw IllegalArgumentException("Unsupported output scheme in mover URL: ${outputUri.scheme} ($outputUri)")
        }
    }

    private fun shouldCompress(uri: URI): Boolean = uri.isQueryParamTrue("compress")

    private fun shouldIncludeVersionInPath(uri: URI): Boolean = uri.isQueryParamTrue("include-version-in-path")

    private fun compress(inputFile: File, outputFile: File, logger: ProgressLogger): File {
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
