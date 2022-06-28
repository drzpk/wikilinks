package dev.drzepka.wikilinks.generator

import com.google.common.collect.HashBiMap
import dev.drzepka.wikilinks.app.db.DatabaseProvider
import dev.drzepka.wikilinks.app.db.FileConfigRepository
import dev.drzepka.wikilinks.common.dump.HttpClientProvider
import dev.drzepka.wikilinks.generator.flow.FlowStep
import dev.drzepka.wikilinks.generator.flow.GeneratorFlow
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.Store
import dev.drzepka.wikilinks.generator.pipeline.downloader.DumpDownloader
import dev.drzepka.wikilinks.generator.pipeline.filter.LinksFilter
import dev.drzepka.wikilinks.generator.pipeline.reader.SqlDumpReader
import dev.drzepka.wikilinks.generator.pipeline.sort.LinksFileSorter
import dev.drzepka.wikilinks.generator.pipeline.writer.LinksDbWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.LinksFileWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.PageWriter
import io.ktor.client.engine.apache.*
import java.io.File
import kotlin.system.exitProcess

private val workingDirectory = File("dumps")

fun main(args: Array<String>) {
    val version = args.getOrNull(0)
    if (version == null) {
        println("Dump version is required as the first parameter.")
        exitProcess(1)
    }

    if (!workingDirectory.isDirectory)
        workingDirectory.mkdir()

    println("Starting generator with dump version=$version")
    val flow = GeneratorFlow(Store())

    flow.segment(DumpDownloader(workingDirectory, version, HttpClientProvider(Apache)))
    flow.step(InitializeDatabaseStep)
    flow.step(PopulatePageTable)
    //flow.step(ExtractPagesFromDbStep)
    flow.step(ExtractLinksFromDumpStep)
    flow.segment(SortLinksFileStep)
    flow.step(PopulateLinksTableStep)
    flow.step(SwapDatabasesStep)

    flow.start()
}

private object InitializeDatabaseStep : FlowStep<Store> {
    override val name = "Initializing the database"

    override fun run(store: Store, logger: ProgressLogger) {
        File(DatabaseProvider.LINKS_DATABASE_NAME).apply {
            if (isFile)
                delete()
        }

        store.db = DatabaseProvider.getLinksDatabase(
            createSchema = true,
            disableProtection = true,
            overrideDirectory = workingDirectory.canonicalPath
        )
    }
}

private object PopulatePageTable : FlowStep<Store> {
    override val name: String = "Populating the page table"

    override fun run(store: Store, logger: ProgressLogger) {
        val writer = PageWriter(store.db)
        val dumpFile = getDumpFileName("page")
        val manager = SqlPipelineManager(dumpFile, { stream -> SqlDumpReader(stream) }, writer)

        manager.start(logger)
        store.pages = writer.pages
    }
}

private object PopulateLinksTableStep : FlowStep<Store> {
    override val name = "Populating the links table"

    override fun run(store: Store, logger: ProgressLogger) {
        val writer = LinksDbWriter(store.db)
        val sourceFile = "dumps/${LinksFileSorter.SORTED_SOURCE_FILE_NAME}"
        val targetFile = "dumps/${LinksFileSorter.SORTED_TARGET_FILE_NAME}"
        val manager = LinksPipelineManager(writer, sourceFile, targetFile)
        manager.start(logger)
    }
}

// Can be used instead of the PopulatePageTable step for testing purposes
@Suppress("unused")
private object ExtractPagesFromDbStep : FlowStep<Store> {
    override val name = "Extracting pages from the database"

    override fun run(store: Store, logger: ProgressLogger) {
        val pages = HashBiMap.create<Int, String>()
        val cursor = store.db.pagesQueries.all().execute()

        while (cursor.next()) {
            val id = cursor.getLong(0)!!
            val title = cursor.getString(1)!!
            pages[id.toInt()] = title
        }

        cursor.close()
        store.pages = pages
    }
}

private object ExtractLinksFromDumpStep : FlowStep<Store> {
    override val name = "Extracting page links from the dump"

    override fun run(store: Store, logger: ProgressLogger) {
        val dumpFile = getDumpFileName("pagelinks")
        val writer = LinksFileWriter(store.pages, workingDirectory)
        val filter = LinksFilter(store.pages)

        val manager = SqlPipelineManager(dumpFile, { stream -> SqlDumpReader(stream) }, writer, filter)
        manager.start(logger)

        // Save some memory
        store.pages = HashBiMap.create()
    }
}

private val SortLinksFileStep = LinksFileSorter(File(workingDirectory, LinksFileWriter.LINKS_FILE_NAME))

private object SwapDatabasesStep : FlowStep<Store> {
    override val name = "Swapping application databases"

    override fun run(store: Store, logger: ProgressLogger) {
        val databasePath = Configuration.databasesDirectory ?: "."
        val databasesDirectory = File(databasePath)
        val configRepository = FileConfigRepository(databasesDirectory.canonicalPath)
        DatabaseSwapper(workingDirectory, File(databasePath), configRepository)
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
