package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.db.DatabaseProvider
import dev.drzepka.wikilinks.app.db.DbLinksRepository
import dev.drzepka.wikilinks.app.model.Path
import dev.drzepka.wikilinks.app.search.LinkSearchService
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

fun cmdLineSearch(args: Array<String>) {
    val startPage = args.getOrNull(0)?.toIntOrNull()
    val targetPage = args.getOrNull(1)?.toIntOrNull()
    if (startPage == null || targetPage == null) {
        println("Usage: <start page> <target page>")
        exit(1)
    }

    searchAndPrint(startPage, targetPage)
}

@OptIn(ExperimentalTime::class)
fun searchAndPrint(startPage: Int, targetPage: Int) {
    println("Searching for paths between pages: $startPage -> $targetPage")

    val mark = TimeSource.Monotonic.markNow()
    val paths = search(startPage, targetPage)
    val duration = mark.elapsedNow()

    if (paths.isNotEmpty()) {
        println("Found ${paths.size} path(s):")
        paths.forEach { println("  ${it.pretty()}") }
    } else {
        println("No paths found")
    }

    println("\nSearch time: ${duration.inWholeMilliseconds / 1000.0} seconds")
}

fun search(startPage: Int, targetPage: Int): List<Path> {
    return getSearchService().findPaths(startPage, targetPage)
}

fun getSearchService(): LinkSearchService {
    val database = DatabaseProvider.getDatabase()
    val repository = DbLinksRepository(database)
    return LinkSearchService(repository)
}
