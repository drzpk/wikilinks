package dev.drzepka.wikilinks.common.model.database

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlin.js.JsName
import kotlin.test.*

class DatabaseFileTest {

    @Test
    @JsName("test1")
    fun `should parse history file name`() {
        assertFalse(DatabaseType.HISTORY.languageSpecific)
        assertFalse(DatabaseType.HISTORY.versioned)

        val parsed = DatabaseFile.parse("history.db")
        assertNotNull(parsed)
        assertEquals("history.db", parsed.fileName)
        assertEquals(DatabaseType.HISTORY, parsed.type)
        assertNull(parsed.language)
        assertNull(parsed.version)
    }

    @Test
    @JsName("test2")
    fun `should parse cache file name`() {
        assertTrue(DatabaseType.CACHE.languageSpecific)
        assertTrue(DatabaseType.CACHE.versioned)

        val parsed = DatabaseFile.parse("cache-en-123.db")
        assertNotNull(parsed)
        assertEquals("cache-en-123.db", parsed.fileName)
        assertEquals(DatabaseType.CACHE, parsed.type)
        assertEquals(DumpLanguage.EN, parsed.language)
        assertEquals("123", parsed.version)
    }

    @Test
    @JsName("test3")
    fun `should parse links file name`() {
        assertTrue(DatabaseType.LINKS.versioned)
        assertTrue(DatabaseType.LINKS.languageSpecific)

        val parsed = DatabaseFile.parse("links-en-1234.db")
        assertNotNull(parsed)
        assertEquals("links-en-1234.db", parsed.fileName)
        assertEquals(DatabaseType.LINKS, parsed.type)
        assertEquals(DumpLanguage.EN, parsed.language)
        assertEquals("1234", parsed.version)
    }

    @Test
    @JsName("test4")
    fun `should not parse file name with wrong extension`() {
        assertFalse(DatabaseType.HISTORY.languageSpecific)
        assertFalse(DatabaseType.HISTORY.versioned)

        val parsed = DatabaseFile.parse("history.dbx")
        assertNull(parsed)
    }

    @Test
    @JsName("test5")
    fun `should not parse versioned file type when file name doesn't contain version`() {
        assertTrue(DatabaseType.LINKS.versioned)
        assertTrue(DatabaseType.LINKS.languageSpecific)

        val parsed = DatabaseFile.parse("links-en.db")
        assertNull(parsed)
    }

    @Test
    @JsName("test6")
    fun `should create not versioned database file`() {
        assertFalse(DatabaseType.HISTORY.versioned)

        val file = DatabaseFile.create(DatabaseType.HISTORY)
        assertEquals(DatabaseType.HISTORY, file.type)
        assertNull(file.version)
        assertEquals("history.db", file.fileName)
    }

    @Test
    @JsName("test7")
    fun `should create cache database file`() {
        assertTrue(DatabaseType.CACHE.languageSpecific)
        assertTrue(DatabaseType.CACHE.versioned)

        val file = DatabaseFile.create(DatabaseType.CACHE, language = DumpLanguage.EN, version = "012")
        assertEquals(DatabaseType.CACHE, file.type)
        assertEquals(DumpLanguage.EN, file.language)
        assertEquals("cache-en-012.db", file.fileName)
    }
}
