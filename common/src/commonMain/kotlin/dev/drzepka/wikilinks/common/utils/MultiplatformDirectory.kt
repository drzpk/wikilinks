package dev.drzepka.wikilinks.common.utils

expect class MultiplatformDirectory(path: String) {
    fun listFiles(): List<MultiplatformFile>
}
