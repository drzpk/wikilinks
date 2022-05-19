package dev.drzepka.wikilinks.generator.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

internal class SqlDumpReaderTest {

    @Test
    fun `should dump values - large buffer size`() {
        testDumpingValues(64 * 1024)
    }

    @Test
    fun `should dump values - small buffer size`() {
        testDumpingValues(24)
    }

    private fun testDumpingValues(bufferSize: Int) {
        val list = getReader(bufferSize).asSequence().toList()

        assertEquals(2, list.size)
        assertEquals("INSERT INTO `pagelinks` VALUES (586,0,'!',0),(4748,0,'!',0),(9773,0,'!',0);", list[0])
        assertEquals("INSERT INTO `something_else` VALUES (1),(2),(3);", list[1])
    }

    private fun getReader(bufferSize: Int): SqlDumpReader {
        val bytes = javaClass.classLoader.getResourceAsStream("links_dump.sql")!!.readAllBytes()
        val contents = String(bytes).replace("\r\n", "\n")

        val byteStream = ByteArrayOutputStream()
        val gzipStream = GZIPOutputStream(byteStream)
        gzipStream.write(contents.encodeToByteArray())
        gzipStream.close()

        return SqlDumpReader(ByteArrayInputStream(byteStream.toByteArray()), bufferSize)
    }
}