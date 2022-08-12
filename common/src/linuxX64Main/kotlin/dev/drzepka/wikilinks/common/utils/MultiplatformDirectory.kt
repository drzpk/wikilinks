package dev.drzepka.wikilinks.common.utils

import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.*

actual class MultiplatformDirectory actual constructor(private val path: String) {
    actual fun listFiles(): List<MultiplatformFile> {
        val dir = opendir(path)

        val files = mutableListOf<MultiplatformFile>()
        try {
            while (true) {
                val file = readdir(dir) ?: break
                val fileValue = file.pointed
                if (fileValue.d_type.toInt() == DT_REG)
                    files.add(MultiplatformFile(path + "/" + fileValue.d_name.toKString()))
            }
        } finally {
            closedir(dir)
        }

        return files
    }

    actual fun mkdirs() {
        val parts = path
            .replace('\\', '/')
            .replace(Regex("""/+"""), "/")
            .split('/')

        var current = if (path.startsWith("/")) "/" else ""
        for (part in parts) {
            current += "$part/"
            mkdir(current, S_IRWXU)
        }
    }
}
