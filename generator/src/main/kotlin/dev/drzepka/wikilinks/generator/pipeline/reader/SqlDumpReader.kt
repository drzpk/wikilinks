package dev.drzepka.wikilinks.generator.pipeline.reader

import java.io.InputStream

class SqlDumpReader(stream: InputStream, bufferCapacity: Int = 32 * 1024) : GZipReader(stream, bufferCapacity) {

    init {
        // Ensure that an insert statement will fit completely in at most 2 buffers.
        // This constraint is needed in the doReadNextLine() method.
        if (bufferCapacity < INSERT_STATEMENT.length)
            throw IllegalArgumentException("Buffer capacity cannot be smaller than ${INSERT_STATEMENT.length}")
    }

    override fun shouldSkipCurrentLine(checkBuilderAndBuffer: Boolean): Boolean = doesNotStartWithInsertStatement(checkBuilderAndBuffer)

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

    companion object {
        private const val INSERT_STATEMENT = "INSERT INTO "
    }
}
