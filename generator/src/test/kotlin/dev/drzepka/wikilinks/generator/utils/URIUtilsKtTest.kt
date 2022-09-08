package dev.drzepka.wikilinks.generator.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

internal class URIUtilsKtTest {

    @Test
    fun `should get file path only if it's valid`() {
        val file = URI.create("file:///path/subpath").getFilePath()
        assertEquals(File("path/subpath"), file)

        val error1 = kotlin.runCatching { URI.create("https://example.con").getFilePath() }
        assertEquals("Expected file URI scheme", error1.exceptionOrNull()?.message)
    }

    @Test
    fun `should recognize if URI query param evaluates to true`() {
        assertTrue(URI.create("xyz://abc?param=true").isQueryParamTrue("param"))
        assertTrue(URI.create("xyz://abc?param=tRuE").isQueryParamTrue("param"))
        assertFalse(URI.create("xyz://abc?param=false").isQueryParamTrue("param"))
        assertFalse(URI.create("xyz://abc?param=something-else").isQueryParamTrue("param"))
        assertFalse(URI.create("xyz://abc?another-param=true").isQueryParamTrue("param"))
    }

    @Test
    fun `should get URI query param value`() {
        assertEquals("simple-value", URI.create("xyz://abc?param=simple-value").getQueryParamValue("param"))
        assertEquals("encoded &@ value", URI.create("xyz://abc?param=encoded%20%26%40%20value").getQueryParamValue("param"))
        assertNull(URI.create("xyz://abc?another=simple-value").getQueryParamValue("param"))
    }
}
