package dev.drzepka.wikilinks.generator.pipeline.link

import dev.drzepka.wikilinks.generator.model.Link
import dev.drzepka.wikilinks.generator.pipeline.reader.Reader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class LinksFileReaderTest {

    @Test
    fun `should group links by first column`() {
        val input = """
            12,192
            12,199
            12,381
            13,12
            15,283
            15,1
        """.trimIndent()

        val groups = getGroups(input, 0)
        assertEquals(3, groups.size)

        assertEquals(3, groups[0].size)
        assertEquals(Link(12, 192), groups[0][0])
        assertEquals(Link(12, 199), groups[0][1])
        assertEquals(Link(12, 381), groups[0][2])

        assertEquals(1, groups[1].size)
        assertEquals(Link(13, 12), groups[1][0])

        assertEquals(2, groups[2].size)
        assertEquals(Link(15, 283), groups[2][0])
        assertEquals(Link(15, 1), groups[2][1])
    }

    @Test
    fun `should group links by second column`() {
        val input = """
            5,2
            6,2
            8,9
            2,9
            1,9
        """.trimIndent()

        val groups = getGroups(input, 1)
        assertEquals(2, groups.size)

        assertEquals(2, groups[0].size)
        assertEquals(Link(5, 2), groups[0][0])
        assertEquals(Link(6, 2), groups[0][1])

        assertEquals(3, groups[1].size)
        assertEquals(Link(8, 9), groups[1][0])
        assertEquals(Link(2, 9), groups[1][1])
        assertEquals(Link(1, 9), groups[1][2])
    }

    @Test
    fun `should ignore empty lines at the end of input`() {
        val input = """
            1,2
            1,3
            
            
        """.trimIndent()

        val groups = getGroups(input, 0)
        assertEquals(1, groups.size)
        assertEquals(Link(1, 2), groups[0][0])
        assertEquals(Link(1, 3), groups[0][1])
    }

    @Test
    fun `should return correct next grouping column value`() {
        val input = """
            1,2
            1,3
            7,4
        """.trimIndent()

        val inputReader = createReader(input)
        val linkReader = LinksFileReader(inputReader, 0)

        assertEquals(1, linkReader.nextGroupingColumnValue)
        linkReader.next()
        assertEquals(7, linkReader.nextGroupingColumnValue)

        linkReader.close()
    }

    private fun getGroups(input: String, groupingColumn: Int): List<List<Link>> {
        val inputReader = createReader(input)
        val linkReader = LinksFileReader(inputReader, groupingColumn)
        val groups = linkReader.asSequence().toList()
        linkReader.close()
        return groups
    }

    private fun createReader(input: String): Reader = object : Reader {
        private val iterator = input.split('\n').iterator()

        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): String = iterator.next()
        override fun close() = Unit
    }
}
