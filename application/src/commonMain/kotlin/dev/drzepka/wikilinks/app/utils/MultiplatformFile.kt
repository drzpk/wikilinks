package dev.drzepka.wikilinks.app.utils

expect class MultiplatformFile(path: String) {
    fun isFile(): Boolean
    fun read(): String
    fun write(content: String)
    fun create()
    fun delete()
}