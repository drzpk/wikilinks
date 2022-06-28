package dev.drzepka.wikilinks.generator.pipeline

import dev.drzepka.wikilinks.generator.model.Value

class SqlStatementParser(private val statement: String) : Iterator<Value> {
    private var pos = 0
    private var buffer: Value? = null
    private val valueParser = ValueParser(statement)

    override fun hasNext(): Boolean {
        if (pos >= statement.length)
            return false

        if (buffer == null)
            extractNextValue()

        return buffer != null
    }

    override fun next(): Value {
        if (buffer == null && !hasNext())
            throw NoSuchElementException("End of statement reached")

        val local = buffer!!
        buffer = null
        return local
    }

    private fun extractNextValue() {
        if (pos == 0)
            moveToFirstValue()

        buffer = valueParser.parse(pos)
        pos = valueParser.pos

        if (!consume(VALUE_SEPARATOR) && !consume(STATEMENT_TERMINATOR))
            throw IllegalStateException("Unexpected character at $pos: '${statement[pos]}'")
    }

    private fun moveToFirstValue() {
        val index = statement.indexOf(VALUES_KEYWORD)
        if (index == -1)
            throw IllegalArgumentException("VALUES keyword wasn't found in the statement")

        pos = index + VALUES_KEYWORD.length
    }

    private fun consume(value: Char): Boolean {
        return if (statement[pos] == value) {
            pos++
            true
        } else false
    }

    companion object {
        private const val VALUES_KEYWORD = " VALUES "
        private const val VALUE_SEPARATOR = ','
        private const val STATEMENT_TERMINATOR = ';'
    }
}
