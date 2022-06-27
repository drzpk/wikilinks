package dev.drzepka.wikilinks.app.utils

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.*

actual class MultiplatformFile actual constructor(private val path: String) {

    actual fun isFile(): Boolean = access(path, F_OK) == 0

    actual fun read(): String {
        val file = fopen(path, "r") ?: throw IllegalArgumentException("Unable to open file for reading")
        val content = StringBuilder()

        try {
            memScoped {
                val len = 8 * 1024
                val buffer = allocArray<ByteVar>(len)
                do {
                    val line = fgets(buffer, len, file)?.toKString()
                    if (line != null)
                        content.append(line)
                } while (line != null)
            }
        } finally {
            fclose(file)
        }

        return content.toString()
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
