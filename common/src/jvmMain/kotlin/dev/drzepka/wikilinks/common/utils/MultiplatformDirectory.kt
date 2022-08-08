package dev.drzepka.wikilinks.common.utils

import java.io.File

actual class MultiplatformDirectory actual constructor(path: String) {
    private val dir = File(path)

    actual fun listFiles(): List<MultiplatformFile> {
        return (dir.listFiles() ?: emptyArray<File>())
            .filter { it.isFile }
            .map { MultiplatformFile(it.absolutePath) }
    }
}
