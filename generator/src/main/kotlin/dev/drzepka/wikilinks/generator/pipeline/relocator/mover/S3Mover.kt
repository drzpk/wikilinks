package dev.drzepka.wikilinks.generator.pipeline.relocator.mover

import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.UploadFileRequest
import software.amazon.awssdk.transfer.s3.progress.TransferListener
import java.io.File

class S3Mover(private val bucketName: String, private val directoryKey: String) : FileMover {
    private val manager = S3TransferManager.builder().build()

    override fun move(file: File, logger: ProgressLogger) {
        val fullKey = "$directoryKey/${file.name}".removePrefix("/")

        val putObjectRequest = PutObjectRequest
            .builder()
            .bucket(bucketName)
            .key(fullKey)
            .build()

        val uploadFileRequest = UploadFileRequest
            .builder()
            .putObjectRequest(putObjectRequest)
            .source(file)
            .overrideConfiguration { it.addListener(FileProgressListener(file, logger)) }
            .build()

        val upload = manager.uploadFile(uploadFileRequest)
        upload.completionFuture().join()
    }

    private class FileProgressListener(file: File, private val logger: ProgressLogger) : TransferListener {
        private val fileSizeMB = (file.length() / 1024 / 1024).toInt()
        private val progressUpdateThreshold = 5 * 1024 * 1024
        private var nextProgressUpdate = 0

        override fun bytesTransferred(context: TransferListener.Context.BytesTransferred) {
            val progress = context.progressSnapshot().bytesTransferred()
            if (progress >= nextProgressUpdate) {
                val currentMB = (progress / 1024 / 1024).toInt()
                logger.updateProgress(currentMB, fileSizeMB, "MB")
                nextProgressUpdate += progressUpdateThreshold
            }
        }
    }
}
