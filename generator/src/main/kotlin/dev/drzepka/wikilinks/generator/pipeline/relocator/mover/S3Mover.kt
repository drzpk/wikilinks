package dev.drzepka.wikilinks.generator.pipeline.relocator.mover

import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.UploadPartRequest
import java.io.File
import java.net.URI

class S3Mover(private val bucketName: String, private val directoryKey: String, endpointHost: String? = null) : FileMover {
    private val s3Client = S3Client
        .builder()
        .endpointOverride(if (endpointHost != null) URI.create("https://$endpointHost") else null)
        .build()

    override fun move(file: File, logger: ProgressLogger) {
        val fullKey = "$directoryKey/${file.name}".removePrefix("/")

        val initRequest = s3Client.createMultipartUpload {
            it.bucket(bucketName)
                .key(fullKey)
        }

        val parts = try {
            readAndUploadParts(file, fullKey, initRequest.uploadId(), logger)
        } catch (e: Exception) {
            println("An error occurred while uploading parts, cancelling the upload")

            s3Client.abortMultipartUpload {
                it.bucket(bucketName)
                    .key(fullKey)
                    .uploadId(initRequest.uploadId())
            }

            throw e
        }

        val completedUpload = CompletedMultipartUpload.builder()
            .parts(parts)
            .build()
        s3Client.completeMultipartUpload {
            it.bucket(bucketName)
                .key(fullKey)
                .uploadId(initRequest.uploadId())
                .multipartUpload(completedUpload)
        }
    }

    private fun readAndUploadParts(
        file: File,
        key: String,
        uploadId: String,
        logger: ProgressLogger
    ): Collection<CompletedPart> = runBlocking {
        val channel = Channel<PartData>(capacity = 2)

        launch {
            readFileParts(file, channel)
        }

        val parts = async {
            uploadParts(channel, key, uploadId, logger)
        }

        parts.await()
    }

    private suspend fun uploadParts(
        channel: ReceiveChannel<PartData>,
        key: String,
        uploadId: String,
        logger: ProgressLogger
    ): List<CompletedPart> {
        var partNo = 1
        var length = 0L
        val completedParts = mutableListOf<CompletedPart>()

        for (content in channel) {
            val request = UploadPartRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNo)
                .build()

            logger.updateProgress(content.position.toMB(), content.length.toMB(), "MB")
            length = content.length

            val requestBody = RequestBody.fromBytes(content.body)
            val response = s3Client.uploadPart(request, requestBody)

            val completedPart = CompletedPart.builder()
                .partNumber(partNo)
                .eTag(response.eTag())
                .checksumCRC32(response.checksumCRC32())
                .checksumCRC32C(response.checksumCRC32())
                .checksumSHA1(response.checksumSHA1())
                .checksumSHA256(response.checksumSHA256())
                .build()

            completedParts.add(completedPart)
            partNo++
        }

        logger.updateProgress(length.toMB(), length.toMB(), "MB")
        return completedParts
    }

    private suspend fun readFileParts(file: File, channel: SendChannel<PartData>) {
        val stream = file.inputStream().buffered(8 * 1024 * 1024)
        val length = file.length()
        var position = 0L

        while (true) {
            val buffer = ByteArray(PART_SIZE)
            val partSize = withContext(Dispatchers.IO) {
                stream.read(buffer)
            }

            if (partSize < 1) {
                channel.close()
                break
            }

            val data = PartData(buffer.copyOf(partSize), position, length)
            channel.send(data)
            position += partSize
        }

        withContext(Dispatchers.IO) {
            stream.close()
        }
    }

    private fun Long.toMB(): Int = (this / 1024.0 / 1024).toInt()

    private class PartData(val body: ByteArray, val position: Long, val length: Long)

    companion object {
        private const val PART_SIZE = 50 * 1024 * 1024
    }
}
