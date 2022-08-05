package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.DumpInfo
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.common.model.searchresult.PageInfo
import dev.drzepka.wikilinks.common.model.searchresult.SearchDuration
import kotlinx.browser.window
import kotlin.js.Promise
import kotlin.random.Random

object MockLinkSearchService : LinkSearchService {

    override fun search(sourcePage: Int, targetPage: Int): Promise<LinkSearchResult> {
        val degrees = Random.nextInt(1, 6)
        val paths = getRandomPaths(degrees)

        val result = LinkSearchResult(
            degrees,
            paths,
            getRandomPageInfo(paths.flatMap { it.pages }.toSet()),
            getRandomDuration(),
            0.5f,
            DumpInfo(DumpLanguage.EN, "latest")
        )

        return Promise { resolve, _ ->
            window.setTimeout({
                resolve.invoke(result)
            }, Random.nextInt(1000, 2000))
        }
    }

    private fun getRandomPaths(degrees: Int): List<Path> {
        val number = Random.nextInt(1, 10)
        return (0 until number).map { getRandomPath(degrees) }
    }

    private fun getRandomPath(degrees: Int): Path = Path((0..degrees).map { Random.nextInt(100, 1_000_000) })

    private fun getRandomPageInfo(pages: Collection<Int>): Map<Int, PageInfo> = pages
        .associateWith { PageInfo(1, "Page Title $it", "https://en.wikipedia.org/wiki/IDontExist") }

    private fun getRandomDuration(): SearchDuration =
        Random.nextInt(600, 30_000).let { totalTime ->
            SearchDuration(
                totalTime,
                Random.nextInt((totalTime * 0.5).toInt(), (totalTime * 0.9).toInt()),
                Random.nextInt((totalTime * 0.2).toInt(), (totalTime * 0.5).toInt())
            )
        }
}
