package dev.drzepka.wikilinks.common.utils

actual class MultiplatformFile actual constructor(path: String) {
    actual fun isFile(): Boolean {
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
