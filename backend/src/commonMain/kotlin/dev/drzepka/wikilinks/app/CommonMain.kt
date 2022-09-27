package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.KoinApp.searchService
import dev.drzepka.wikilinks.app.utils.exit
import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin

fun commonMain(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "http")
        httpApplication()
    else
        cmdLineSearch(args)
}

fun cmdLineSearch(args: Array<String>) {
    val startPage = args.getOrNull(0)?.toIntOrNull()
    val targetPage = args.getOrNull(1)?.toIntOrNull()
    if (startPage == null || targetPage == null) {
        println("Usage: <start page> <target page> [language]")
        exit(1)
    }

    searchAndPrint(startPage, targetPage, getLanguage(args.getOrNull(2)))
}

private fun getLanguage(raw: String?): DumpLanguage {
    if (raw != null)
        return DumpLanguage.fromString(raw) ?: throw IllegalArgumentException("Unsupported language: $raw")

    return DumpLanguage.EN
}

private fun searchAndPrint(startPage: Int, targetPage: Int, language: DumpLanguage) {
    println("Searching for paths between pages: $startPage -> $targetPage")

    val result = search(startPage, targetPage, language)
    val paths = result.first

    if (paths.isNotEmpty()) {
        println("Found ${paths.size} path(s):")
        paths.forEach { println("  ${it.pretty()}") }
    } else {
        println("No paths found")
    }

    println("\nSearch duration: ${result.second / 1000.0} seconds")
}

private fun search(startPage: Int, targetPage: Int, language: DumpLanguage): Pair<List<Path>, Long> {
    startKoin {
        modules(coreModule())
    }

    return runBlocking {
        searchService.simpleSearch(language, startPage, targetPage)
    }
}
