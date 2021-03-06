package dev.drzepka.wikilinks.common.dump

import dev.drzepka.wikilinks.common.model.dump.ArchiveDump
import dev.drzepka.wikilinks.common.testRunBlocking
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class DumpResolverTest {
    private val variants = listOf("page", "pagelinks")

    @Test
    @JsName("test1")
    fun `should find last dump`() {
        val ds = "https://dump.source.org/test"

        val mockEngine = MockEngine {
            val url = it.url.toString()
            when {
                url == ds -> respondOk(DUMP_LISTING_CONTENT)
                it.method == HttpMethod.Head && url.endsWith("20220620-page.sql.gz") -> respondWithLength(123)
                it.method == HttpMethod.Head && url.endsWith("20220620-pagelinks.sql.gz") -> respondWithLength(456)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val resolver = DumpResolver(HttpClientProvider(mockEngine), ds, variants)
        val dumps = testRunBlocking { resolver.resolveLatestDumps() }
        val archives = dumps.dumps

        assertEquals("20220620", dumps.version)
        assertEquals(2, archives.size)
        assertTrue(archives.contains(ArchiveDump("$ds/20220620/enwiki-20220620-page.sql.gz", 123, false)))
        assertTrue(archives.contains(ArchiveDump("$ds/20220620/enwiki-20220620-pagelinks.sql.gz", 456, false)))
    }

    @Test
    @JsName("test2")
    fun `should fall back to previous dump if the last one is missing required files`() {
        val ds = "https://dump.source.org/test"

        val mockEngine = MockEngine {
            val url = it.url.toString()
            when {
                url == ds -> respondOk(DUMP_LISTING_CONTENT)
                it.method == HttpMethod.Head && url.endsWith("20220620-page.sql.gz") -> respondWithLength(1)
                it.method == HttpMethod.Head && url.endsWith("20220601-page.sql.gz") -> respondWithLength(2)
                it.method == HttpMethod.Head && url.endsWith("20220601-pagelinks.sql.gz") -> respondWithLength(3)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        val resolver = DumpResolver(HttpClientProvider(mockEngine), ds, variants)
        val dumps = testRunBlocking { resolver.resolveLatestDumps() }
        val archives = dumps.dumps

        assertEquals("20220601", dumps.version)
        assertEquals(2, archives.size)
        assertTrue(archives.contains(ArchiveDump("$ds/20220601/enwiki-20220601-page.sql.gz", 2, false)))
        assertTrue(archives.contains(ArchiveDump("$ds/20220601/enwiki-20220601-pagelinks.sql.gz", 3, false)))
    }

    private fun MockRequestHandleScope.respondWithLength(length: Long): HttpResponseData {
        return respond("", headers = headersOf(HttpHeaders.ContentLength, length.toString()))
    }

    companion object {
        private val DUMP_LISTING_CONTENT = """
            <html>
            <head>
                <title>Index of /enwiki/</title>
            </head>

            <body bgcolor="white">
                <h1>Index of /enwiki/</h1>
                <hr>
                <pre><a href="../">../</a>
                    <a href="20220320/">20220320/</a>           02-May-2022 01:27                   -
                    <a href="20220401/">20220401/</a>           21-May-2022 01:30                   -
                    <a href="20220420/">20220420/</a>           02-Jun-2022 01:27                   -
                    <a href="20220501/">20220501/</a>           21-Jun-2022 01:30                   -
                    <a href="20220520/">20220520/</a>           22-May-2022 00:58                   -
                    <a href="20220601/">20220601/</a>           07-Jun-2022 16:08                   -
                    <a href="20220620/">20220620/</a>           21-Jun-2022 09:10                   -
                    <a href="latest/">latest/</a>               21-Jun-2022 09:09                   -
                </pre>
                <hr>
            </body>
            </html>
        """.trimIndent()
    }
}
