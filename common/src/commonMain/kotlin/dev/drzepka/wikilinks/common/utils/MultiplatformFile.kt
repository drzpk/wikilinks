package dev.drzepka.wikilinks.common.utils

import kotlinx.datetime.Instant

expect class MultiplatformFile(path: String) {
    fun getName(): String
    fun getFullPath(): String
    fun isFile(): Boolean
    fun getModificationTime(): Instant?
    fun read(): String
    fun readBytes(): ByteArray
    fun write(content: String)
    fun create()
    fun delete()
}
