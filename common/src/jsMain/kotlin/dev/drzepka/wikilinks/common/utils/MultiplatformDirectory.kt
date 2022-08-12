package dev.drzepka.wikilinks.common.utils

actual class MultiplatformDirectory actual constructor(path: String) {
    actual fun listFiles(): List<MultiplatformFile> = throwException()
    actual fun mkdirs() {
        throwException()
    }

    private fun throwException(): Nothing = throw NotImplementedError("Not available on JS platform")
}
