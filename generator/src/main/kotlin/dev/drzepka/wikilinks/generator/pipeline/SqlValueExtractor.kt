package dev.drzepka.wikilinks.generator.pipeline

class SqlValueExtractor {
    private var values = mutableListOf<String>()
    private var statement = ""
    private var statementPos = 0

    fun extractFromStatement(statement: String): List<String> {
        values = mutableListOf()
        this.statement = statement
        statementPos = 0

        moveToFirstValue()

        while (true) {
            val value = readNextValue() ?: break
            values.add(value)
        }

        return values
    }

    private fun moveToFirstValue() {
        val index = statement.indexOf(VALUES_KEYWORD)
        if (index == -1)
            throw IllegalArgumentException("VALUES keyword wasn't found in the statement")

        statementPos = index + VALUES_KEYWORD.length
    }

    private fun readNextValue(): String? {
        if (endOfStatementReached())
            return null

        var valueEnd = statement.indexOf(VALUE_SEPARATOR, statementPos)
        if (valueEnd == -1)
            valueEnd = statement.indexOf(LAST_VALUE_ENDING, statementPos)
        if (valueEnd == -1)
            throw IllegalStateException("Closing parenthesis wasn't found")

        val value = statement.substring(statementPos until valueEnd)
        statementPos = valueEnd + VALUE_SEPARATOR.length


        return value
    }

    private fun endOfStatementReached(): Boolean = statementPos >= statement.length

    companion object {
        private const val VALUES_KEYWORD = " VALUES ("
        private const val VALUE_SEPARATOR = "),("
        private const val LAST_VALUE_ENDING = ");"
    }
}
