package dev.drzepka.wikilinks.generator.pipeline

class ValueParser(private val source: String) {
    private val values = mutableListOf<Any?>()
    private var pos = 0

    fun parse(): List<Any?> {
        while (pos < source.length) {
            if (isNumber())
                readNumber()
            else if (isString())
                readString()
            else if (isNull())
                readNull()
            else if (isParameterSeparator())
                pos++
            else
                throwSyntaxError(pos)
        }

        return values
    }

    private fun isNumber(): Boolean = source[pos] in DIGIT_ZERO..DIGIT_NINE

    private fun isString(): Boolean = source[pos] == APOSTROPHE

    private fun isNull(): Boolean =
        pos + NULL.length <= source.length && source.substring(pos, pos + NULL.length) == NULL

    private fun isParameterSeparator(): Boolean = source[pos] == PARAMETER_SEPARATOR

    private fun readNumber() {
        var endIndex = pos + 1
        while (endIndex < source.length && source[endIndex] != PARAMETER_SEPARATOR)
            endIndex++

        val raw = source.substring(pos, endIndex)
        val number = parseNumber(raw)

        values.add(number)
        pos = endIndex
    }

    private fun parseNumber(input: String): Number {
        return try {
            if (input.contains(DECIMAL_SEPARATOR))
                input.toDouble()
            else
                input.toInt()
        } catch (e: NumberFormatException) {
            throw IllegalStateException("Error while parsing source: $source", e)
        }
    }

    private fun readString() {
        // Skip the opening apostrophe
        pos++

        var endIndex = pos
        while (endIndex + 1 < source.length && source[endIndex] != APOSTROPHE) {
            if (source[endIndex] == ESCAPE_CHARACTER)
                endIndex++
            endIndex++
        }

        if (source[endIndex] != APOSTROPHE)
            throwSyntaxError(endIndex, "unexpected end of string")


        val builder = StringBuilder(endIndex - pos)
        var i = pos
        while (i < endIndex) {
            if (source[i] == ESCAPE_CHARACTER)
                i++
            builder.append(source[i++])
        }

        values.add(builder.toString())

        // Also skip the closing apostrophe
        pos = endIndex + 1
    }

    private fun readNull() {
        values.add(null)
        pos += 4
    }

    private fun throwSyntaxError(at: Int, additionalInfo: String? = null): Nothing {
        val suffix = if (additionalInfo != null) ": $additionalInfo" else ""
        throw IllegalStateException("Syntax error at $at$suffix. Source: $source")
    }

    companion object {
        private const val APOSTROPHE = '\''
        private const val ESCAPE_CHARACTER = '\\'
        private const val DIGIT_ZERO = '0'
        private const val DIGIT_NINE = '9'
        private const val NULL = "NULL"
        private const val PARAMETER_SEPARATOR = ','
        private const val DECIMAL_SEPARATOR = '.'
    }
}
