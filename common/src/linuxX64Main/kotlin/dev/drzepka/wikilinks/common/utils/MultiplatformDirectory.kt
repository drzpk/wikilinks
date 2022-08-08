package dev.drzepka.wikilinks.common.utils

import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.DT_REG
import platform.posix.closedir
import platform.posix.opendir
import platform.posix.readdir

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
}
