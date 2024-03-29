package dev.drzepka.wikilinks.app.service.search

import dev.drzepka.wikilinks.app.db.LinksRepository
import dev.drzepka.wikilinks.app.model.PageVertex
import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import mu.KotlinLogging

class PathFinderService(
    private val linksRepository: LinksRepository,
    maxSearchDepth: Int? = null
) {
    private val log = KotlinLogging.logger {}
    private val maxSearchDepth = maxSearchDepth ?: DEFAULT_MAX_SEARCH_DEPTH

    suspend fun findPaths(language: DumpLanguage, sourcePage: Int, targetPage: Int): List<Path> {
        // "Head" as in search head. When searching from source or target side,
        // the head moves towards the other end until it meets with the other one.
        var sourceHead: MutableMap<Int, PageVertex> = HashMap()
        var targetHead: MutableMap<Int, PageVertex> = HashMap()

        sourceHead[sourcePage] = PageVertex(sourcePage)
        targetHead[targetPage] = PageVertex(targetPage)

        var sourceDepth = 0
        var targetDepth = 0

        while (sourceDepth + targetDepth < maxSearchDepth) {
            val outLinksCount = linksRepository.getOutLinksCount(language, *sourceHead.keys.toIntArray())
            val inLinksCount = linksRepository.getInLinksCount(language, *targetHead.keys.toIntArray())

            log.trace { "out link count from ${sourceHead.keys}: $outLinksCount" }
            log.trace { "in link count from ${targetHead.keys}: $inLinksCount" }

            if (outLinksCount <= inLinksCount) {
                sourceDepth++
                sourceHead = moveSearchHead(sourceHead, language, false)
            } else {
                targetDepth++
                targetHead = moveSearchHead(targetHead, language, true)
            }

            val paths = constructPaths(sourceHead, targetHead)
            if (paths.isNotEmpty()) {
                log.info { "Found ${paths.size} paths between pages $sourcePage and $targetPage" }
                return paths
            }
        }

        log.warn { "Maximum search depth of $maxSearchDepth has been reached for search: $sourcePage -> $targetPage" }
        return emptyList()
    }

    private suspend fun moveSearchHead(
        oldHead: MutableMap<Int, PageVertex>,
        language: DumpLanguage,
        reverseDirection: Boolean
    ): MutableMap<Int, PageVertex> {
        val links = if (!reverseDirection)
            linksRepository.getOutLinks(language, *oldHead.keys.toIntArray())
        else
            linksRepository.getInLinks(language, *oldHead.keys.toIntArray())

        log.trace { "Found ${links.size} links. Direction: ${if (!reverseDirection) "forward" else "reverse"}" }

        val newHead = HashMap<Int, PageVertex>()
        links.forEach { link ->
            val sourcePage = if (!reverseDirection) link.from else link.to
            val targetPage = if (!reverseDirection) link.to else link.from

            val parentVertex = oldHead[sourcePage]!!
            if (targetPage in newHead) {
                val existing = newHead[targetPage]!!
                existing.addParent(parentVertex)
            } else {
                val new = PageVertex(targetPage, parentVertex)
                newHead[targetPage] = new
            }
        }

        return newHead
    }

    private fun constructPaths(sourceHead: Map<Int, PageVertex>, targetHead: Map<Int, PageVertex>): List<Path> {
        val commonPages = sourceHead.keys.intersect(targetHead.keys)
        if (commonPages.isEmpty())
            return emptyList()

        val paths = mutableListOf<Path>()
        for (commonPage in commonPages) {
            val sourcePaths = sourceHead[commonPage]!!.unfold()
            val targetPaths = targetHead[commonPage]!!.unfold()

            for (sourcePath in sourcePaths) {
                for (targetPath in targetPaths)
                    paths.add(joinPaths(sourcePath, targetPath))
            }
        }

        return paths
    }

    private fun joinPaths(left: Path, right: Path): Path {
        val pages = ArrayList<Int>(left.pages.size + right.pages.size - 1)
        pages.addAll(left.pages)

        for (i in right.pages.size - 2 downTo 0)
            pages.add(right.pages[i])

        return Path(pages)
    }

    companion object {
        private const val DEFAULT_MAX_SEARCH_DEPTH = 20
    }
}
