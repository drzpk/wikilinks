package dev.drzepka.wikilinks.app.search

import dev.drzepka.wikilinks.app.db.LinksRepository
import dev.drzepka.wikilinks.app.model.PageVertex
import dev.drzepka.wikilinks.app.model.Path
import mu.KotlinLogging

class LinkSearchService(
    private val linksRepository: LinksRepository,
    maxSearchDepth: Int? = null
) {
    private val log = KotlinLogging.logger {}
    private val maxSearchDepth = maxSearchDepth ?: DEFAULT_MAX_SEARCH_DEPTH

    fun findPaths(sourcePage: Int, targetPage: Int): List<Path> {
        val targetVertices = ArrayList<PageVertex>()
        val queue = HashSet<PageVertex>()

        val sourceVertex = PageVertex(sourcePage)
        queue.add(sourceVertex)

        var searchDepth = maxSearchDepth
        var depth = -1

        while (queue.isNotEmpty() && targetVertices.isEmpty()) {
            depth++
            val vertices = queue.associateByTo(hashMapOf()) { it.page }
            queue.clear()

            for (vertex in vertices.values) {
                if (vertex.page == targetPage) {
                    targetVertices.add(vertex)

                    // The shortest possible path length has been found, no point in going deeper
                    searchDepth = depth
                }
            }

            if (depth < searchDepth) {
                val pages = vertices.values.map { it.page }.toIntArray()
                linksRepository.getOutLinks(*pages).forEach { link ->

                    val parentVertex = vertices[link.from]!!
                    val thisVertex = if (link.to in vertices) {
                        val existing = vertices[link.to]!!
                        existing.addParent(parentVertex)
                        existing
                    } else {
                        val new = PageVertex(link.to, parentVertex)
                        vertices[link.to] = new
                        new
                    }

                    queue.add(thisVertex)
                }
            }
        }

        if (depth == maxSearchDepth && targetVertices.isEmpty())
            log.warn { "Maximum search depth of $maxSearchDepth has been reached for search: $sourcePage -> $targetPage" }

        return targetVertices.flatMap { it.unfold() }
    }

    companion object {
        private const val DEFAULT_MAX_SEARCH_DEPTH = 20
    }
}
