package dev.drzepka.wikilinks.generator.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class ValueParserTest {

    @Test
    fun `should parse source`() {
        val source = "10,0,'AccessibleComputing','',1,0,0.33167112649574004,'20220430060236','20220430080215',1002250816,111,'wikitext',NULL"
        val values = ValueParser().parse(source)

        assertEquals(13, values.size)
        assertEquals(10, values[0])
        assertEquals(0, values[1])
        assertEquals("AccessibleComputing", values[2])
        assertEquals("", values[3])
        assertEquals(1, values[4])
        assertEquals(0, values[5])
        assertEquals(0.33167112649574004, values[6] as Double, 0.000001)
        assertEquals("20220430060236", values[7])
        assertEquals("20220430080215", values[8])
        assertEquals(1002250816, values[9])
        assertEquals(111, values[10])
        assertEquals("wikitext", values[11])
        assertNull(values[12])
    }

    @Test
    fun `should parse escaped strings`() {
        val source = "0,'\\'Ndrangheta','C：\\\\',123"
        val values = ValueParser().parse(source)

        assertEquals("'Ndrangheta", values[1])
        assertEquals("C：\\", values[2])
    }
}
