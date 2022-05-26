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
        val queue = ArrayDeque<PageVertex>()

        val sourceVertex = PageVertex(sourcePage, null)
        queue.add(sourceVertex)

        var searchDepth = maxSearchDepth

        while (queue.isNotEmpty()) {
            val vertex = queue.removeFirst()
            log.trace { "$vertex" }

            if (vertex.page == targetPage) {
                targetVertices.add(vertex)

                // The shortest possible path length has been found, no point in going deeper
                searchDepth = vertex.depth
            }

            if (vertex.depth < searchDepth) {
                linksRepository.getOutLinks(vertex.page).forEach { link ->
                    queue.add(PageVertex(link, vertex))
                }
            }
        }

        if (searchDepth == maxSearchDepth)
            log.warn { "Maximum search depth of $DEFAULT_MAX_SEARCH_DEPTH has been reached for search: $sourcePage -> $targetPage" }

        return targetVertices.map { it.unfold() }
    }

    companion object {
        private const val DEFAULT_MAX_SEARCH_DEPTH = 20
    }
}
