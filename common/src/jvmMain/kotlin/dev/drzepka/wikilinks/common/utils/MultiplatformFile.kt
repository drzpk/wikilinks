package dev.drzepka.wikilinks.common.utils

import java.io.File

actual class MultiplatformFile actual constructor(path: String) {
    private val file = File(path)

    actual fun isFile(): Boolean = file.isFile

    actual fun read(): String = file.readText()

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