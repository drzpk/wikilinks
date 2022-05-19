package dev.drzepka.wikilinks.generator.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SqlValueExtractorTest {

    @Test
    fun `should extract values from INSERT statements`() {
        val statement1 = "INSERT INTO `pagelinks` VALUES (586,0,'!',0),(4748,0,'!',0),(9773,0,'!',0);"
        val statement2 = "INSERT INTO `something_else` VALUES (1),(2),(3);"

        val extractor = SqlValueExtractor()
        val list1 = extractor.extractFromStatement(statement1)
        val list2 = extractor.extractFromStatement(statement2)

        assertEquals(3, list1.size)
        assertEquals(3, list2.size)

        assertEquals("586,0,'!',0", list1[0])
        assertEquals("4748,0,'!',0", list1[1])
        assertEquals("9773,0,'!',0", list1[2])

        assertEquals("1", list1[0])
        assertEquals("2", list1[1])
        assertEquals("3", list1[2])
    }
}
