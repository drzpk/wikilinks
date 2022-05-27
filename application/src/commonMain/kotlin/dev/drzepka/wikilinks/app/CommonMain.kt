package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.db.DatabaseProvider
import dev.drzepka.wikilinks.app.db.DbLinksRepository
import dev.drzepka.wikilinks.app.model.Path
import dev.drzepka.wikilinks.app.search.LinkSearchService

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
    val paths = search(startPage, targetPage)

    if (paths.isNotEmpty()) {
        println("Found ${paths.size} path(s):")
        paths.forEach { println("  ${it.pretty()}") }
    } else {
        println("No paths found")
    }
}

fun search(startPage: Int, targetPage: Int): List<Path> {
    return getSearchService().findPaths(startPage, targetPage)
}

private fun getSearchService(): LinkSearchService {
    val database = DatabaseProvider.getDatabase()
    val repository = DbLinksRepository(database)
    return LinkSearchService(repository)
}
