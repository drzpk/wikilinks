package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.DatabaseProvider
import dev.drzepka.wikilinks.DatabaseProvider.databaseName
import dev.drzepka.wikilinks.generator.pipeline.sort.LinksFileSorter
import dev.drzepka.wikilinks.generator.pipeline.writer.LinksWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.PageWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.Writer
import java.io.File

fun main() {
    File(databaseName).apply {
        if (isFile)
            delete()
    }

    val db = DatabaseProvider.getDatabase()
    val pageWriter = PageWriter(db)
    buildTable("page", pageWriter)

    buildTable("links", LinksWriter(pageWriter.pages))

    val file = File("dumps/id_links.txt.gz")
    LinksFileSorter(file).sort()
}

private fun buildTable(name: String, writer: Writer) {
    val dumpFile = getDumpFileName(name)
    val manager = PipelineManager(dumpFile, name, writer)
    manager.start()
}

private fun getDumpFileName(name: String): String {
    val dirName = "dumps"
    val directory = File(dirName)
    val file = directory.listFiles()!!
        .map { it.name }
        .find { it.startsWith("enwiki-") && it.endsWith("$name.sql.gz") }!!
    return "$dirName/$file"
}
