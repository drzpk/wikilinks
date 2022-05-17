package dev.drzepka.wikilinks.generator.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

internal class SqlDumpReaderTest {

    @Test
    fun `should dump values`() {
        val list = getReader().asSequence().toList()

        assertEquals(6, list.size)
        assertEquals("586,0,'!',0", list[0])
        assertEquals("4748,0,'!',0", list[1])
        assertEquals("9773,0,'!',0", list[2])
        assertEquals("1", list[3])
        assertEquals("2", list[4])
        assertEquals("3", list[5])
    }

    private fun getReader(): SqlDumpReader {
        val bytes = javaClass.classLoader.getResourceAsStream("links_dump.sql")!!.readAllBytes()
        val contents = String(bytes).replace("\r\n", "\n")

        val byteStream = ByteArrayOutputStream()
        val gzipStream = GZIPOutputStream(byteStream)
        gzipStream.write(contents.encodeToByteArray())
        gzipStream.close()

        return SqlDumpReader(ByteArrayInputStream(byteStream.toByteArray()))
    }
}