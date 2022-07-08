package dev.drzepka.wikilinks.generator.pipeline.downloader

import dev.drzepka.wikilinks.common.WikiConfig.REQUIRED_FILE_VARIANTS
import dev.drzepka.wikilinks.common.dump.DumpResolver
import dev.drzepka.wikilinks.common.dump.HttpClientProvider
import dev.drzepka.wikilinks.common.model.dump.ArchiveDump
import dev.drzepka.wikilinks.generator.Configuration
import dev.drzepka.wikilinks.generator.flow.FlowSegment
import dev.drzepka.wikilinks.generator.flow.Logger
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.Store
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class DumpDownloader(
    private val workingDirectory: File,
    provider: HttpClientProvider
) : FlowSegment<Store> {
    override val numberOfSteps = if (!Configuration.skipDownloadingDumps) 2 + REQUIRED_FILE_VARIANTS.size else 0

    private val resolver = DumpResolver.createFromConfig(provider)
    private val http = provider.client

    override fun run(store: Store, logger: Logger) = runBlocking {
        if (Configuration.skipDownloadingDumps)
            return@runBlocking

        logger.startNextStep("Resolving new dumps")
        val dumps = resolver.resolveForVersion(store.version)

        logger.startNextStep("Deleting old dumps")
        deleteOldDumps(dumps.dumps.map { it.fileName })

        for ((index, dump) in dumps.dumps.withIndex()) {
            val name = dump.fileName
            logger.startNextStep("Downloading file ${index + 1}/${dumps.dumps.size} ($name)")
            downloadFile(dump, logger)?.join()
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

    private fun CoroutineScope.downloadFile(dump: ArchiveDump, logger: ProgressLogger): Job? {
        val file = File(workingDirectory, dump.fileName)
        if (file.isFile && file.length() == dump.size) {
            println("File is already downloaded, skipping")
            return null
        }

        var offset = 0L
        if (file.isFile && dump.supportsHttpRange) {
            offset = file.length()
            println("File is partially downloaded, resuming download from $offset")
        }

        val channel = writer(file, offset > 0)
        return downloader(dump, offset, logger, channel)
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun CoroutineScope.writer(file: File, append: Boolean) = actor<ByteReadPacket>(context = Dispatchers.IO) {
        val stream = BufferedOutputStream(FileOutputStream(file, append), 1024 * 1024)
        for (packet in channel) {
            stream.writePacket(packet)
        }
        stream.close()
    }

    private fun CoroutineScope.downloader(
        dump: ArchiveDump,
        offset: Long,
        logger: ProgressLogger,
        output: SendChannel<ByteReadPacket>
    ) = launch {
        var totalLengthMB: Int? = null

        http.prepareGet(dump.url) {
            if (offset > 0) {
                // Ranges are zero-based and inclusive (RFC-7233)
                val range = "bytes=$offset-${dump.size - 1}"
                headers.append(HttpHeaders.Range, range)
            }
        }.execute {
            totalLengthMB = it.contentLength()?.let { l -> l / 1024 / 1024 }?.toInt()
            val channel = it.bodyAsChannel()
            var readBytes = 0L

            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(1024 * 1024)
                readBytes += packet.remaining
                output.send(packet)

                val readMB = (readBytes / 1024 / 1024).toInt()
                if (totalLengthMB != null)
                    logger.updateProgress(readMB, totalLengthMB!!, "MB")
                else
                    print("\rDownloaded size: $readMB MB")
            }

            output.close()
        }

        if (totalLengthMB != null)
            logger.updateProgress(totalLengthMB!!, totalLengthMB!!, "MB")
        else
            println()
    }
}
