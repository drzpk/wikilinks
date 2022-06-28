package dev.drzepka.wikilinks.generator.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SqlStatementParserTest {

    @Test
    fun `should extract values from INSERT statements`() {
        val statement1 = "INSERT INTO `pagelinks` VALUES (586,0,'!',0),(4748,0,'!',0),(9773,0,'!',0);"
        val statement2 = "INSERT INTO `something_else` VALUES (1),(2),(3);"

        val list1 = SqlStatementParser(statement1).asSequence().toList()
        val list2 = SqlStatementParser(statement2).asSequence().toList()

        assertEquals(listOf(listOf(586, 0, "!", 0), listOf(4748, 0, "!", 0), listOf(9773, 0, "!", 0)), list1)
        assertEquals(listOf(listOf(1), listOf(2), listOf(3)), list2)
    }

    @Test
    fun `should not detect value separators inside strings`() {
        val statement = "INSERT INTO `test` VALUES " +
                "(40435962,0,'Bhongir_(Lok_Sabha_constituency),(Assembly_constituency)',2),(1,2,3);"

        val list = SqlStatementParser(statement).asSequence().toList()

        assertEquals(2, list.size)
        assertEquals(listOf(40435962, 0, "Bhongir_(Lok_Sabha_constituency),(Assembly_constituency)", 2), list[0])
        assertEquals(listOf(1, 2, 3), list[1])
    }
}
