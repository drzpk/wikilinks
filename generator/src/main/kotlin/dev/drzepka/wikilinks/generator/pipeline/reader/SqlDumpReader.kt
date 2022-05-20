package dev.drzepka.wikilinks.generator.pipeline.reader

import java.io.Closeable
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

class SqlDumpReader(fileStream: InputStream, bufferCapacity: Int = 32 * 1024) : Iterator<String>, Closeable {
    private val reader: InputStreamReader
    private val buffer = CharArray(bufferCapacity)
    private val lineBuilder = StringBuilder()

    private var bufferSize = 0
    private var bufferPos = 0
    private var lineStartPos = 0
    private var line: String? = null
    private var eosReached = false

    init {
        // Ensure that an insert statement will fit completely in at most 2 buffers.
        // This constraint is needed in the doReadNextLine() method.
        if (bufferCapacity < INSERT_STATEMENT.length)
            throw IllegalArgumentException("Buffer capacity cannot be smaller than ${INSERT_STATEMENT.length}")

        val gzipStream = GZIPInputStream(fileStream, bufferCapacity)
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
        if (doesNotStartWithInsertStatement(false)) {
            skipCurrentLine()
            return
        }

        var checkInsertStatement = true

        while (!eosReached && buffer[bufferPos] != EOL_CHAR) {
            bufferPos++
            if (endOfBufferReached()) {
                appendToBuilder()
                fetchNextBlock()

                if (checkInsertStatement) {
                    if (doesNotStartWithInsertStatement(true)) {
                        skipCurrentLine()
                        return
                    }

                    checkInsertStatement = false
                }
            }
        }

        appendToBuilder()
        moveToNextLine()

        line = lineBuilder.toString()
        lineBuilder.clear()
    }

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

    private fun doesNotStartWithInsertStatement(checkBuilderAndBuffer: Boolean): Boolean {
        if (checkBuilderAndBuffer) {
            // Check both the builder and the buffer.
            // First part of the insert statement is in the builder and the other in the buffer.
            if (lineBuilder.length + bufferSize - bufferPos >= INSERT_STATEMENT.length) {
                for (i in INSERT_STATEMENT.indices) {
                    if (i < lineBuilder.length) {
                        if (lineBuilder[i] != INSERT_STATEMENT[i])
                            return true
                    } else {
                        if (buffer[bufferPos + i - lineBuilder.length] != INSERT_STATEMENT[i])
                            return true
                    }
                }
            } else {
                // Line size is too small, it definitely doesn't contain the statement.
                return true
            }
        } else {
            // Check the buffer only
            if (bufferSize - bufferPos >= INSERT_STATEMENT.length) {
                // Insert statement can fit into the buffer, check whether it's actually there
                for (i in INSERT_STATEMENT.indices) {
                    if (buffer[bufferPos + i] != INSERT_STATEMENT[i])
                        return true
                }
            }
        }

        return false
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
        private const val INSERT_STATEMENT = "INSERT INTO "
    }
}
