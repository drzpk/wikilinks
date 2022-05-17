package dev.drzepka.wikilinks.generator.pipeline

import java.io.Closeable
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

class SqlDumpReader(fileStream: InputStream) : Iterator<String>, Closeable {
    private val reader: InputStreamReader
    private val streamBuffer = CharArray(32 * 1024)
    private var bufferSize = 0
    private var bufferPos = 0
    private var eosReached = false

    private val valueBuffer = StringBuffer()
    private var readingValues = false
    private var value: String? = null

    init {
        val gzipStream = GZIPInputStream(fileStream, streamBuffer.size)
        reader = InputStreamReader(gzipStream)
        nextChar()
    }

    override fun hasNext(): Boolean {
        if (value == null)
            readNextValue()
        return !eosReached
    }

    override fun next(): String {
        if (value == null)
            readNextValue()

        if (value == null)
            throw NoSuchElementException()

        val result = value!!
        value = null
        return result
    }

    override fun close() {
        reader.close()
    }

    private fun readNextValue() {
        if (eosReached)
            return

        if (readingValues) {
            readNextValueFromInsertStatement()
        } else {
            val status = consumeUntilInsertStatement() && consumeUntilValuesKeyword()
            if (!status) {
                eosReached = true
                return
            }

            readingValues = true
            readNextValueFromInsertStatement()
        }
    }

    private fun readNextValueFromInsertStatement() {
        while (!valueBuffer.endsWith(VALUE_EOL) && !valueBuffer.endsWith(VALUE_SEPARATOR)) {
            val c = currentChar()
            valueBuffer.append(c)

            if (!nextChar())
                throw IllegalStateException("Unexpected end of file while reading values from the INSERT statement")
        }

        if (valueBuffer.endsWith(VALUE_EOL)) {
            value = valueBuffer.substring(0, valueBuffer.length - VALUE_EOL.length)
            readingValues = false
        } else {
            value = valueBuffer.substring(0, valueBuffer.length - VALUE_SEPARATOR.length)
        }

        valueBuffer.delete(0, valueBuffer.length)
    }

    private fun consumeUntilInsertStatement(): Boolean = consumeUntilString(INSERT_STATEMENT, true)

    private fun consumeUntilValuesKeyword(): Boolean = consumeUntilString(VALUES_KEYWORD, false)

    private fun consumeUntilString(str: String, skipLine: Boolean): Boolean {
        while (!consumeString(str)) {
            val skipped = if (skipLine) consumeUntilNewLine() else nextChar()
            if (!skipped) {
                // End of stream reached
                return false
            }
        }

        return true
    }

    private fun consumeString(str: String): Boolean {
        for (stringChar in str) {
            val currentChar = currentChar()
            if (stringChar == currentChar)
                nextChar()
            else
                return false
        }

        return true
    }

    private fun consumeUntilNewLine(): Boolean {
        while (currentChar() != NEW_LINE_CHAR) {
            if (!nextChar())
                return false
        }

        // Consume the new line character itself
        return nextChar()
    }

    private fun currentChar(): Char = streamBuffer[bufferPos]

    private fun nextChar(): Boolean {
        if (!endOfBufferReached()) {
            bufferPos++
            return true
        }

        bufferSize = reader.read(streamBuffer)
        if (bufferSize == -1)
            return false

        bufferPos = 0
        return true
    }

    private fun endOfBufferReached(): Boolean = bufferPos == bufferSize

    companion object {
        private const val INSERT_STATEMENT = "INSERT INTO "
        private const val VALUES_KEYWORD = " VALUES ("
        private const val VALUE_SEPARATOR = "),("
        private const val VALUE_EOL = ");\n"
        private const val NEW_LINE_CHAR = '\n'
    }
}
