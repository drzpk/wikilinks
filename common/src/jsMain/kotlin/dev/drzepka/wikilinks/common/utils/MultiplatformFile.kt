package dev.drzepka.wikilinks.common.utils

import kotlinx.datetime.Instant

actual class MultiplatformFile actual constructor(path: String) {
    actual fun getName(): String {
        throwException()
    }

    actual fun getFullPath(): String {
        throwException()
    }

    actual fun isFile(): Boolean {
        throwException()
    }

    actual fun getModificationTime(): Instant? {
        throwException()
    }

    actual fun read(): String {
        throwException()
    }

    actual fun readBytes(): ByteArray {
        throwException()
    }

    actual fun write(content: String) {
        throwException()
    }

    actual fun create() {
        throwException()
    }

    actual fun delete() {
        throwException()
    }

    private fun throwException(): Nothing = throw NotImplementedError("Not available on JS platform")
}
