package dev.drzepka.wikilinks.common.utils

import kotlinx.cinterop.*
import kotlinx.datetime.Instant
import platform.posix.*

actual class MultiplatformFile actual constructor(private val path: String) {

    actual fun isFile(): Boolean = access(path, F_OK) == 0

    actual fun getModificationTime(): Instant? {
        if (!isFile())
            return null

        val time = memScoped {
            val statResult = alloc<stat>()
            stat(path, statResult.ptr)
            Pair(statResult.st_mtim.tv_sec, statResult.st_mtim.tv_nsec)
        }

        return Instant.fromEpochSeconds(time.first, time.second)
    }

    actual fun read(): String {
        return readBytes().decodeToString()
    }

    actual fun readBytes(): ByteArray {
        val file = fopen(path, "rb") ?: throw IllegalArgumentException("Unable to open file for reading")

        return try {
            memScoped {
                fseek(file, 0, SEEK_END)
                val len = ftell(file)
                rewind(file)

                val buffer = allocArray<ByteVar>(len)
                fread(buffer, len.toULong(), 1, file)
                buffer.readBytes(len.toInt())
            }
        } finally {
            fclose(file)
        }
    }

    actual fun write(content: String) {
        val file = fopen(path, "w") ?: throw IllegalArgumentException("Unable to open file for writing")

        try {
            memScoped {
                if (fputs(content, file) == EOF)
                    throw IllegalStateException("Error while writing file")
            }
        } finally {
            fclose(file)
        }
    }

    actual fun create() {
        val f = open(path, O_WRONLY or O_CREAT or O_TRUNC)
        close(f)
    }

    actual fun delete() {
        remove(path)
    }
}
