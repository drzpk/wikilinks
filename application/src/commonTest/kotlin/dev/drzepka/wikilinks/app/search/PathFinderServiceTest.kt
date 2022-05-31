package dev.drzepka.wikilinks.app.search

import dev.drzepka.wikilinks.app.db.InMemoryLinksRepository
import dev.drzepka.wikilinks.app.model.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

internal class PathFinderServiceTest {

    @Test
    fun `should find single shortest path`() {
        val links = mapOf(
            1 to listOf(2, 3),
            2 to listOf(10, 15),
            15 to listOf(100, 150, 190),
            3 to listOf(1000, 1500),
            1000 to listOf(2000, 1),
            2000 to listOf(150)
        )

        val service = createService(links)
        val paths = service.findPaths(1, 150)

        assertEquals(1, paths.size)
        assertContains(paths, Path(1, 2, 15, 150))
    }

    @Test
    fun `should find multiple shortest paths`() {
        val links = mapOf(
            0 to listOf(10, 11, 1000),
            10 to listOf(20, 21),
            11 to listOf(22),
            20 to listOf(30, 21),
            21 to listOf(30),
            22 to listOf(30),
            1000 to listOf(1100, 1200),
            1100 to listOf(10, 1300),
            1300 to listOf(30)
        )

        val service = createService(links)
        val paths = service.findPaths(0, 30)

        assertEquals(3, paths.size)
        assertContains(paths, Path(0, 10, 20, 30))
        assertContains(paths, Path(0, 10, 21, 30))
        assertContains(paths, Path(0, 11, 22, 30))
    }

    @Test
    fun `should not find path if there isn't one`() {
        val links = mapOf(
            0 to listOf(10, 11),
            10 to listOf(20, 21),
            11 to listOf(22),
            20 to listOf(30),
            21 to listOf(30),
            22 to listOf(30),
            500 to listOf(501, 502),
            502 to listOf(510, 511),
            511 to listOf(520)
        )

        val service = createService(links)

        assertEquals(0, service.findPaths(0, 500).size)
        assertEquals(0, service.findPaths(0, 520).size)
        assertEquals(0, service.findPaths(502, 10).size)
    }

    @Test
    fun `should not find path if max search depth has been exceeded`() {
        val links = mapOf(
            1 to listOf(2),
            2 to listOf(3),
            3 to listOf(4),
            4 to listOf(5),
            5 to listOf(6)
        )

        val service = createService(links, 4)

        assertEquals(1, service.findPaths(1, 5).size)
        assertEquals(0, service.findPaths(1, 6).size)
    }

    private fun createService(outLinks: Map<Int, List<Int>>, maxSearchDepth: Int? = null): PathFinderService {
        val repository = InMemoryLinksRepository()
        outLinks.forEach { repository.addLinks(it.key, *it.value.toIntArray()) }
        return PathFinderService(repository, maxSearchDepth)
    }
}
