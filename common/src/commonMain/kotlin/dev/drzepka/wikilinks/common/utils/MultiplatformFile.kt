package dev.drzepka.wikilinks.common.utils

expect class MultiplatformFile(path: String) {
    fun isFile(): Boolean
    fun read(): String
    fun readBytes(): ByteArray
    fun write(content: String)
    fun create()
    fun delete()
}