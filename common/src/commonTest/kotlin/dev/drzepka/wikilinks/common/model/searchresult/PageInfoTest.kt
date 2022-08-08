package dev.drzepka.wikilinks.common.model.searchresult

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class PageInfoTest {

    @Test
    @JsName("test1")
    fun `should encode url without special characters`() {
        val info = PageInfo.create(1, "Title", "desc", null, DumpLanguage.EN)
        assertEquals("https://en.wikipedia.org/wiki/Title", info.url)
    }

    @Test
    @JsName("test2")
    fun `should encode url with spaces`() {
        val info = PageInfo.create(1, "First Second", "desc", null, DumpLanguage.EN)
        assertEquals("https://en.wikipedia.org/wiki/First_Second", info.url)
    }
}
