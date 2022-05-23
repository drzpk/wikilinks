package dev.drzepka.wikilinks.generator.pipeline.sort

import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

class BlockFile(file: File, private val sortColumn: Int) : Comparable<BlockFile>, Closeable {
    private val reader: BufferedReader
    private var sortValue = 0

    var line = ""
        private set

    init {
        val gzipStream = GZIPInputStream(FileInputStream(file))
        reader = BufferedReader(InputStreamReader(gzipStream), 16 * 1024 * 1024)

        if (!next())
            throw IllegalStateException("File is empty")
    }

    fun next(): Boolean {
        val tmpLine = reader.readLine()
        if (tmpLine?.isEmpty() != false)
            return false

        line = tmpLine
        sortValue = line.split(",")[sortColumn].toInt()
        return true
    }

    override fun compareTo(other: BlockFile): Int = sortValue.compareTo(other.sortValue)

    override fun close() {
        reader.close()
    }
}
