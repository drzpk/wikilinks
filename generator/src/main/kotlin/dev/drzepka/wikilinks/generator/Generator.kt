package dev.drzepka.wikilinks.generator

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlDriver
import dev.drzepka.wikilinks.app.db.DatabaseProvider
import dev.drzepka.wikilinks.app.db.FileConfigRepository
import dev.drzepka.wikilinks.common.dump.HttpClientProvider
import dev.drzepka.wikilinks.generator.flow.FileFlowStorage
import dev.drzepka.wikilinks.generator.flow.FlowStep
import dev.drzepka.wikilinks.generator.flow.GeneratorFlow
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.Store
import dev.drzepka.wikilinks.generator.pipeline.downloader.DumpDownloader
import dev.drzepka.wikilinks.generator.pipeline.pagelookup.PageLookupFactory
import dev.drzepka.wikilinks.generator.pipeline.processor.LinksProcessor
import dev.drzepka.wikilinks.generator.pipeline.reader.SqlDumpReader
import dev.drzepka.wikilinks.generator.pipeline.sort.LinksFileSorter
import dev.drzepka.wikilinks.generator.pipeline.writer.LinksDbWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.LinksFileWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.PageWriter
import io.ktor.client.engine.apache.*
import java.io.File
import java.lang.management.ManagementFactory

private val workingDirectory = File(Configuration.workingDirectory)

fun generate(version: String) {
    println("Starting generator with dump version=$version")
    println("Available CPUs: ${availableProcessors()}")
    println("Max heap: ${ManagementFactory.getMemoryMXBean().heapMemoryUsage.max}")

    val storage = FileFlowStorage(version, workingDirectory)
    val store = Store(storage).apply {
        this.version = version
    }
    val flow = GeneratorFlow(store)

    flow.segment(DumpDownloader(workingDirectory, HttpClientProvider(Apache)))
    flow.step(InitializeDatabaseStep)
    flow.step(PopulatePageTable)
    flow.step(ExtractLinksFromDumpStep)
    flow.step(ClearPageLookup)
    flow.segment(LinksFileSorter(File(workingDirectory, LinksFileWriter.LINKS_FILE_NAME)))
    flow.step(PopulateLinksTableStep)
    flow.step(SwapDatabasesStep)
    flow.step(DeleteTemporaryDataStep)

    flow.start()
}

private object InitializeDatabaseStep : FlowStep<Store> {
    override val name = "Initializing the database"

    override fun run(store: Store, logger: ProgressLogger) {
        val key = "InitializeDatabaseStep"
        if (store[key] == null) {
            File(DatabaseProvider.LINKS_DATABASE_NAME).apply {
                if (isFile)
                    delete()
            }
            store[key] = "done"
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
        store.pageLookup = PageLookupFactory.create(store.db)

        val key = "PopulatePageTableStep"
        if (store[key] == null) {
            populate(store, logger)
            store[key] = "done"
        } else {
            println("Page table already populated, loading existing data into memory")
            loadFromDb(store)
        }
    }

    private fun populate(store: Store, logger: ProgressLogger) {
        val writer = PageWriter(store.pageLookup, store.db)
        val dumpFile = getDumpFile("page")
        val manager = SqlPipelineManager(dumpFile, { stream -> SqlDumpReader(stream) }, writer)
        manager.start(logger)
    }

    private fun loadFromDb(store: Store) {
        val cursor = store.db.pagesQueries.all().execute()

        while (cursor.next()) {
            val id = cursor.getLong(0)!!
            val title = cursor.getString(1)!!
            store.pageLookup.save(id.toInt(), title)
        }

        cursor.close()
    }
}

private object PopulateLinksTableStep : FlowStep<Store> {
    override val name = "Populating the links table"

    override fun run(store: Store, logger: ProgressLogger) {
        val key = "PopulateLinksTableStep"
        if (store[key] != null) {
            println("Links table has already been populated, skipping")
            return
        }

        val writer = LinksDbWriter(store.db)
        val sourceFile = File(workingDirectory, LinksFileSorter.SORTED_SOURCE_FILE_NAME)
        val targetFile = File(workingDirectory, LinksFileSorter.SORTED_TARGET_FILE_NAME)
        val manager = LinksPipelineManager(writer, sourceFile, targetFile)
        manager.start(logger)
        store[key] = "done"
    }
}

private object ExtractLinksFromDumpStep : FlowStep<Store> {
    override val name = "Extracting page links from the dump"

    override fun run(store: Store, logger: ProgressLogger) {
        val key = "ExtractLinksFromDumpStep"
        if (store[key] != null) {
            println("Step has already been executed, skipping")
            return
        }

        val dumpFile = getDumpFile("pagelinks")
        val writer = LinksFileWriter(workingDirectory)
        val processor = LinksProcessor(store.pageLookup)

        val manager = SqlPipelineManager(dumpFile, { stream -> SqlDumpReader(stream) }, writer, processor)
        manager.start(logger)
        store[key] = "done"
    }
}

private object ClearPageLookup : FlowStep<Store> {
    override val name = "Clearing page lookup"

    override fun run(store: Store, logger: ProgressLogger) {
        // Save some memory
        try {
            store.pageLookup.clear()
        } catch (ignored: UninitializedPropertyAccessException) {
        }
    }
}

private object SwapDatabasesStep : FlowStep<Store> {
    override val name = "Swapping application databases"

    override fun run(store: Store, logger: ProgressLogger) {
        val key = "SwapDatabasesStep"
        if (store[key] != null) {
            println("Databases have already been swapped, skipping")
            return
        }

        closeDatabaseConnection(store.db)

        val databasePath = Configuration.databasesDirectory ?: "."
        val databasesDirectory = File(databasePath)
        val configRepository = FileConfigRepository(databasesDirectory.canonicalPath)

        DatabaseSwapper(workingDirectory, File(databasePath), configRepository).run(store.version)
        store[key] = "done"
    }

    private fun closeDatabaseConnection(db: Transacter) {
        // Let's cheat for now
        val field = TransacterImpl::class.java.getDeclaredField("driver")
        field.isAccessible = true
        val driver = field.get(db) as SqlDriver
        driver.close()
    }
}

private object DeleteTemporaryDataStep : FlowStep<Store> {
    override val name = "Deleting temporary data"

    override fun run(store: Store, logger: ProgressLogger) {
        workingDirectory.listFiles()!!
            .filter { it.name.endsWith(".gz") }
            .forEach {
                println("Deleting temporary file $it")
                it.delete()
            }
    }
}

private fun getDumpFile(name: String): File {
    return workingDirectory.listFiles()!!
        .find { it.name.startsWith("enwiki-") && it.name.endsWith("$name.sql.gz") }!!
}
