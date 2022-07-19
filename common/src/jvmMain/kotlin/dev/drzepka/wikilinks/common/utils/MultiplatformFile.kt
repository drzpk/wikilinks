package dev.drzepka.wikilinks.common.utils

import kotlinx.datetime.Instant
import java.io.File

actual class MultiplatformFile actual constructor(path: String) {
    private val file = File(path)

    actual fun isFile(): Boolean = file.isFile

    actual fun getModificationTime(): Instant? {
        if (!isFile())
            return null

        val millis = file.lastModified()
        return Instant.fromEpochMilliseconds(millis)
    }

    actual fun read(): String = file.readText()

    actual fun readBytes(): ByteArray = file.readBytes()

    actual fun write(content: String) {
        file.writer().use {
            it.write(content)
        }
    }

    actual fun create() {
        file.createNewFile()
    }

    actual fun delete() {
        file.delete()
    }
}
