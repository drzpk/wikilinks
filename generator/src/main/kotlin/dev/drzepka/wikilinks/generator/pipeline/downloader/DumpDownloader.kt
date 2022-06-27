package dev.drzepka.wikilinks.generator.pipeline.downloader

import dev.drzepka.wikilinks.generator.Configuration
import dev.drzepka.wikilinks.generator.flow.FlowSegment
import dev.drzepka.wikilinks.generator.flow.Logger
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.ResolvedDump
import dev.drzepka.wikilinks.generator.model.Store
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import kotlinx.coroutines.runBlocking
import java.io.File

class DumpDownloader(private val workingDirectory: File, provider: HttpClientProvider) : FlowSegment<Store> {
    override val numberOfSteps = if (Configuration.skipDownloadingDumps) 2 + REQUIRED_FILE_VARIANTS.size else 0

    private val resolver = LastDumpResolver(provider, REQUIRED_FILE_VARIANTS)
    private val http = provider.client

    override fun run(store: Store, logger: Logger) = runBlocking {
        if (Configuration.skipDownloadingDumps)
            return@runBlocking

        logger.startNextStep("Resolving new dumps")
        val dumps = resolver.resolveLastDumpFileUrls()

        logger.startNextStep("Deleting old dumps")
        deleteOldDumps(dumps.map { it.fileName })

        for ((index, dump) in dumps.withIndex()) {
            val name = dump.fileName
            logger.startNextStep("Downloading file ${index + 1}/${dumps.size} ($name)")
            downloadFile(dump, logger)
        }
    }

    private fun deleteOldDumps(currentDumpNames: List<String>) {
        workingDirectory.listFiles()!!
            .filter { it.name.endsWith(".gz") && it.name !in currentDumpNames }
            .forEach {
                println("Deleting file: $it")
                if (!it.delete())
                    throw IllegalStateException("Unable to delete file $it")
            }
    }

    private suspend fun downloadFile(dump: ResolvedDump, logger: ProgressLogger) {
        val file = File(workingDirectory, dump.fileName)
        if (file.isFile && file.length() == dump.size) {
            println("File already exist, skipping")
            return
        }

        val fileChannel = file.writeChannel()
        var totalLengthMB: Int? = null

        http.prepareGet(dump.url).execute {
            totalLengthMB = it.contentLength()?.let { l -> l / 1024 / 1024 }?.toInt()
            val channel = it.bodyAsChannel()
            var readBytes = 0L

            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                readBytes += packet.remaining
                fileChannel.writePacket(packet)

                val readMB = (readBytes / 1024 / 1024).toInt()
                if (totalLengthMB != null)
                    logger.updateProgress(readMB, totalLengthMB!!, "MB")
                else
                    print("\rDownloaded size: $readMB MB")
            }
        }

        if (totalLengthMB != null)
            logger.updateProgress(totalLengthMB!!, totalLengthMB!!, "MB")
        else
            println()
    }

    companion object {
        private val REQUIRED_FILE_VARIANTS = listOf("page", "pagelinks")
    }
}
