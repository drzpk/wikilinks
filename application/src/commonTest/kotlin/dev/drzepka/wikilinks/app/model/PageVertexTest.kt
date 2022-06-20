package dev.drzepka.wikilinks.app.model

import dev.drzepka.wikilinks.common.model.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

internal class PageVertexTest {

    @Test
    fun `should unfold graph to all paths`() {
        // Level 0
        val root = PageVertex(0)
        // Level 1
        val v10 = PageVertex(10, root)
        val v11 = PageVertex(11, root)
        // Level 2
        val v20 = PageVertex(20, v10)
        val v21 = PageVertex(21, listOf(v10, v20))
        val v22 = PageVertex(22, v11)
        // Level 3
        val v30 = PageVertex(30, listOf(v20, v21, v22))

        val paths = v30.unfold()

        assertEquals(3, paths.size)
        assertContains(paths, Path(0, 10, 20, 30))
        assertContains(paths, Path(0, 10, 21, 30))
        assertContains(paths, Path(0, 11, 22, 30))
    }

    @Test
    fun `should unfold graph with with circular parents`() {
        val v0 = PageVertex(0)
        val v10 = PageVertex(10, v0)
        val v20 = PageVertex(20, v10)
        val v21 = PageVertex(21, v10)
        val v30 = PageVertex(30, listOf(v20, v21))

        // Add circular dependencies
        v10.addParent(v20)
        v10.addParent(v21)

        val paths = v30.unfold()

        assertEquals(2, paths.size)
        assertContains(paths, Path(0, 10, 20, 30))
        assertContains(paths, Path(0, 10, 21, 30))
    }
}
