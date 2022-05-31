package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.db.DatabaseProvider
import dev.drzepka.wikilinks.app.db.DbLinksRepository
import dev.drzepka.wikilinks.app.db.DbPagesRepository
import dev.drzepka.wikilinks.app.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.app.search.LinkSearchService
import dev.drzepka.wikilinks.app.search.PageInfoService
import dev.drzepka.wikilinks.app.search.PathFinderService

fun cmdLineSearch(args: Array<String>) {
    val startPage = args.getOrNull(0)?.toIntOrNull()
    val targetPage = args.getOrNull(1)?.toIntOrNull()
    if (startPage == null || targetPage == null) {
        println("Usage: <start page> <target page>")
        exit(1)
    }

    searchAndPrint(startPage, targetPage)
}

fun searchAndPrint(startPage: Int, targetPage: Int) {
    println("Searching for paths between pages: $startPage -> $targetPage")

    val result = search(startPage, targetPage)
    val paths = result.paths

    if (paths.isNotEmpty()) {
        println("Found ${paths.size} path(s):")
        paths.forEach { println("  ${it.pretty()}") }
    } else {
        println("No paths found")
    }

    println("\nSearch duration: ${result.duration.totalMillis / 1000.0} seconds")
}

fun search(startPage: Int, targetPage: Int): LinkSearchResult {
    return getSearchService().search(startPage, targetPage)
}

fun getSearchService(): LinkSearchService {
    val database = DatabaseProvider.getDatabase()
    val linksRepository = DbLinksRepository(database)
    val pagesRepository = DbPagesRepository(database)

    val pathFinderService = PathFinderService(linksRepository)
    val pageInfoService = PageInfoService(pagesRepository)

    return LinkSearchService(pathFinderService, pageInfoService)
}
