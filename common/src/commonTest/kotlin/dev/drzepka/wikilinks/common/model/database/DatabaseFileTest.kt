package dev.drzepka.wikilinks.common.model.database

import kotlin.js.JsName
import kotlin.test.*

class DatabaseFileTest {

    @Test
    @JsName("test1")
    fun `should parse not versioned database file name`() {
        assertFalse(DatabaseType.HISTORY.versioned)

        val parsed = DatabaseFile.parse("history.db")
        assertNotNull(parsed)
        assertEquals("history.db", parsed.fileName)
        assertEquals(DatabaseType.HISTORY, parsed.type)
        assertNull(parsed.version)
    }

    @Test
    @JsName("test2")
    fun `should parse versioned database file name`() {
        assertTrue(DatabaseType.LINKS.versioned)

        val parsed = DatabaseFile.parse("links-1234.db")
        assertNotNull(parsed)
        assertEquals("links-1234.db", parsed.fileName)
        assertEquals(DatabaseType.LINKS, parsed.type)
        assertEquals("1234", parsed.version)
    }

    @Test
    @JsName("test3")
    fun `should not parse file name with wrong extension`() {
        assertFalse(DatabaseType.HISTORY.versioned)

        val parsed = DatabaseFile.parse("history.dbx")
        assertNull(parsed)
    }

    @Test
    @JsName("test4")
    fun `should not parse versioned file type when file name doesn't contain version`() {
        assertTrue(DatabaseType.LINKS.versioned)

        val parsed = DatabaseFile.parse("links.db")
        assertNull(parsed)
    }

    @Test
    @JsName("test5")
    fun `should create not versioned database file`() {
        assertFalse(DatabaseType.HISTORY.versioned)

        val file = DatabaseFile.create(DatabaseType.HISTORY)
        assertEquals(DatabaseType.HISTORY, file.type)
        assertNull(file.version)
        assertEquals("history.db", file.fileName)
    }

    @Test
    @JsName("test6")
    fun `should create versioned database file`() {
        assertTrue(DatabaseType.LINKS.versioned)

        val file = DatabaseFile.create(DatabaseType.LINKS, version = "890")
        assertEquals(DatabaseType.LINKS, file.type)
        assertEquals("890", file.version)
        assertEquals("links-890.db", file.fileName)
    }
}
