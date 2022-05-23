package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.DatabaseProvider
import dev.drzepka.wikilinks.DatabaseProvider.databaseName
import dev.drzepka.wikilinks.generator.flow.FlowStep
import dev.drzepka.wikilinks.generator.flow.GeneratorFlow
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.Store
import dev.drzepka.wikilinks.generator.pipeline.sort.LinksFileSorter
import dev.drzepka.wikilinks.generator.pipeline.writer.LinksWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.PageWriter
import java.io.File

fun main() {
    val flow = GeneratorFlow(Store())

    flow.step(InitializeDatabaseStep)
    flow.step(PopulatePageTable)
    //flow.step(ExtractPagesFromDbStep)
    flow.step(ExtractLinksFromDumpStep)
    flow.segment(LinksFileSorter(File("dumps/id_links.txt.gz")))

    flow.start()
}

private object InitializeDatabaseStep : FlowStep<Store> {
    override val name = "Initializing the database"

    override fun run(store: Store, logger: ProgressLogger) {
        File(databaseName).apply {
            if (isFile)
                delete()
        }

        store.db = DatabaseProvider.getDatabase()
    }
}

private object PopulatePageTable : FlowStep<Store> {
    override val name: String = "Populating the page table"

    override fun run(store: Store, logger: ProgressLogger) {
        val writer = PageWriter(store.db)
        val dumpFile = getDumpFileName("page")
        val manager = PipelineManager(dumpFile, writer)

        manager.start(logger)
        store.pages = writer.pages
    }
}

// Can be used instead of the PopulatePageTable step for testing purposes
@Suppress("unused")
private object ExtractPagesFromDbStep : FlowStep<Store> {
    override val name = "Extracting pages from the database"

    override fun run(store: Store, logger: ProgressLogger) {
        val pages = HashMap<String, Int>()
        val cursor = store.db.pagesQueries.all().execute()

        while (cursor.next()) {
            val id = cursor.getLong(0)!!
            val title = cursor.getString(1)!!
            pages[title] = id.toInt()
        }

        cursor.close()
        store.pages = pages
    }
}

private object ExtractLinksFromDumpStep : FlowStep<Store> {
    override val name = "Extracting page links from the dump"

    override fun run(store: Store, logger: ProgressLogger) {
        val dumpFile = getDumpFileName("pagelinks")
        val writer = LinksWriter(store.pages)
        val manager = PipelineManager(dumpFile, writer)
        manager.start(logger)
    }
}

private fun getDumpFileName(name: String): String {
    val dirName = "dumps"
    val directory = File(dirName)
    val file = directory.listFiles()!!
        .map { it.name }
        .find { it.startsWith("enwiki-") && it.endsWith("$name.sql.gz") }!!
    return "$dirName/$file"
}
