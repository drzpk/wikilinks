package dev.drzepka.wikilinks.generator.pipeline.reader

import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

open class GZipReader(stream: InputStream, bufferCapacity: Int = 32 * 1024) : Reader {
    protected val buffer = CharArray(bufferCapacity)
    protected val lineBuilder = StringBuilder()
    protected var bufferSize = 0
    protected var bufferPos = 0

    private val reader: InputStreamReader
    private var lineStartPos = 0
    private var line: String? = null
    private var eosReached = false

    init {
        val gzipStream = GZIPInputStream(stream, bufferCapacity)
        reader = InputStreamReader(gzipStream)
        fetchNextBlock()
    }

    override fun hasNext(): Boolean {
        if (line == null)
            readNextLine()
        return !eosReached
    }

    override fun next(): String {
        if (line == null)
            readNextLine()

        if (eosReached)
            throw NoSuchElementException()

        val result = line!!
        line = null
        return result
    }

    override fun close() {
        reader.close()
    }

    private fun readNextLine() {
        while (!eosReached && line?.isEmpty() != false)
            doReadNextLine()
    }

    private fun doReadNextLine() {
        if (shouldSkipCurrentLine(false)) {
            skipCurrentLine()
            return
        }

        var checkSkipCondition = true

        while (!eosReached && buffer[bufferPos] != EOL_CHAR) {
            bufferPos++
            if (endOfBufferReached()) {
                appendToBuilder()
                fetchNextBlock()

                if (checkSkipCondition) {
                    if (shouldSkipCurrentLine(true)) {
                        skipCurrentLine()
                        return
                    }

                    checkSkipCondition = false
                }
            }
        }

        appendToBuilder()
        moveToNextLine()

        line = lineBuilder.toString()
        lineBuilder.clear()
    }

    protected open fun shouldSkipCurrentLine(checkBuilderAndBuffer: Boolean): Boolean = false

    private fun skipCurrentLine() {
        moveToNextLine()
        line = null
        lineBuilder.clear()
    }

    private fun moveToNextLine() {
        while (!eosReached && buffer[bufferPos] != EOL_CHAR) {
            bufferPos++
            if (endOfBufferReached())
                fetchNextBlock()
        }

        // Skip the new line character
        bufferPos++
        lineStartPos = bufferPos

        if (endOfBufferReached())
            fetchNextBlock()
    }

    private fun endOfBufferReached(): Boolean = bufferPos >= bufferSize

    private fun appendToBuilder() {
        if (bufferPos - lineStartPos > 0)
            lineBuilder.append(buffer, lineStartPos, bufferPos - lineStartPos)
    }

    private fun fetchNextBlock() {
        bufferSize = reader.read(buffer)
        if (bufferSize == -1)
            eosReached = true

        bufferPos = 0
        lineStartPos = 0
    }

    companion object {
        private const val EOL_CHAR = '\n'
    }
}
