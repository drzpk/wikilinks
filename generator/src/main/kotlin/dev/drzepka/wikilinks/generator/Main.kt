package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.DatabaseProvider
import dev.drzepka.wikilinks.DatabaseProvider.databaseName
import dev.drzepka.wikilinks.generator.pipeline.PageWriter
import java.io.File

@Suppress("UnstableApiUsage")
fun main() {
    File(databaseName).apply {
        if (isFile)
            delete()
    }

    val db = DatabaseProvider.getDatabase()
    val fileName = "dumps/enwiki-20220501-page.sql.gz"

    val writer = PageWriter(db)
    val manager = PipelineManager(fileName, "page", writer)
    manager.start()
}
