package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.DatabaseProvider
import dev.drzepka.wikilinks.DatabaseProvider.databaseName
import dev.drzepka.wikilinks.generator.pipeline.writer.AbstractWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.PageWriter
import java.io.File

@Suppress("UnstableApiUsage")
fun main() {
    File(databaseName).apply {
        if (isFile)
            delete()
    }

    val db = DatabaseProvider.getDatabase()
    buildTable("page", PageWriter(db))
}

private fun buildTable(name: String, writer: AbstractWriter) {
    val dumpFile = getDumpFileName(name)
    val manager = PipelineManager(dumpFile, name, writer)
    manager.start()
}

private fun getDumpFileName(name: String): String {
    val dirName = "dumps"
    val directory = File(dirName)
    val file = directory.list()!!.find { it.startsWith("enwiki-") && it.endsWith("$name.sql.gz") }!!
    return "$dirName/$file"
}
