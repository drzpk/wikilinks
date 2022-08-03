package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.app.db.DatabaseProvider
import dev.drzepka.wikilinks.common.dump.HttpClientProvider
import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.generator.flow.FileFlowStorage
import dev.drzepka.wikilinks.generator.flow.FlowStep
import dev.drzepka.wikilinks.generator.flow.GeneratorFlow
import dev.drzepka.wikilinks.generator.flow.ProgressLogger
import dev.drzepka.wikilinks.generator.model.Store
import dev.drzepka.wikilinks.generator.pipeline.downloader.DumpDownloader
import dev.drzepka.wikilinks.generator.pipeline.lookup.InMemoryRedirectLookup
import dev.drzepka.wikilinks.generator.pipeline.lookup.PageLookupFactory
import dev.drzepka.wikilinks.generator.pipeline.processor.LinksProcessor
import dev.drzepka.wikilinks.generator.pipeline.reader.SqlDumpReader
import dev.drzepka.wikilinks.generator.pipeline.sort.LinksFileSorter
import dev.drzepka.wikilinks.generator.pipeline.writer.LinksDbWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.LinksFileWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.PageRedirectsWriter
import dev.drzepka.wikilinks.generator.pipeline.writer.PageWriter
import io.ktor.client.engine.apache.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

private val workingDirectory = File(Configuration.workingDirectory)
private val databaseProvider = DatabaseProvider()

fun generate(language: DumpLanguage, version: String) {
    println("Starting generator with language=$language and version=$version")

    val storage = FileFlowStorage(version, workingDirectory)
    val store = Store(storage).apply {
        linksDatabaseFile = DatabaseFile.create(DatabaseType.LINKS, language, version)
    }
    val flow = GeneratorFlow(store)

    flow.segment(DumpDownloader(workingDirectory, HttpClientProvider(Apache)))
    flow.step(InitializeDatabaseStep)
    flow.step(PopulatePageTable)
    flow.step(PopulatePageRedirects)
    flow.step(LoadLookupsFromDatabase)
    flow.step(ExtractLinksFromDumpStep)
    flow.step(ClearLookups)
    flow.segment(LinksFileSorter(File(workingDirectory, LinksFileWriter.LINKS_FILE_NAME)))
    flow.step(PopulateLinksTableStep)
    flow.step(MoveDatabaseStep)
    flow.step(DeleteTemporaryDataStep)

    flow.start()
}

private object InitializeDatabaseStep : FlowStep<Store> {
    override val name = "Initializing the database"

    override fun run(store: Store, logger: ProgressLogger) {
        val key = "InitializeDatabaseStep"
        if (store[key] == null) {
            File(store.linksDatabaseFile.fileName).apply {
                if (isFile)
                    delete()
            }
            store[key] = "done"
        }

        store.db = databaseProvider.getLinksDatabase(
            store.linksDatabaseFile.language!!,
            fixedVersion = store.linksDatabaseFile.version!!,
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
            LoadLookupsFromDatabase.loadPageLookup = true
        }
    }

    private fun populate(store: Store, logger: ProgressLogger) {
        val writer = PageWriter(store.pageLookup, store.db)
        val dumpFile = getDumpFile(store, "page")
        val manager = SqlPipelineManager(dumpFile, { stream -> SqlDumpReader(stream) }, writer)
        manager.start(logger)
    }
}

private object PopulatePageRedirects : FlowStep<Store> {
    override val name = "Populating page redirects"

    override fun run(store: Store, logger: ProgressLogger) {
        store.redirectLookup = InMemoryRedirectLookup()

        val key = "PopulatePageRedirects"
        if (store[key] == null) {
            populate(store, logger)
            store[key] = "done"
        } else {
            println("Redirects have already been populated, skipping")
            LoadLookupsFromDatabase.loadRedirectLookup = true
        }
    }

    private fun populate(store: Store, logger: ProgressLogger) {
        val writer = PageRedirectsWriter(store.pageLookup, store.redirectLookup, store.db)
        val dumpFile = getDumpFile(store, "redirect")
        val manager = SqlPipelineManager(dumpFile, { stream -> SqlDumpReader(stream) }, writer)
        manager.start(logger)
    }
}

private object LoadLookupsFromDatabase : FlowStep<Store> {
    override val name = "Loading lookups from database"

    var loadPageLookup = false
    var loadRedirectLookup = false

    override fun run(store: Store, logger: ProgressLogger) {
        if (!loadPageLookup && !loadRedirectLookup) {
            println("Nothing to load, skipping")
            return
        }

        val cursor = store.db.pagesQueries.all().execute()
        while (cursor.next()) {
            val id = cursor.getLong(0)!!
            val title = cursor.getString(1)!!
            val redirectsTo = cursor.getLong(2)

            if (loadPageLookup)
                store.pageLookup.save(id.toInt(), title)
            if (loadRedirectLookup && redirectsTo != null)
                store.redirectLookup[id.toInt()] = redirectsTo.toInt()
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

        val dumpFile = getDumpFile(store, "pagelinks")
        val writer = LinksFileWriter(workingDirectory)
        val processor = LinksProcessor(store.pageLookup, store.redirectLookup)

        val manager = SqlPipelineManager(dumpFile, { stream -> SqlDumpReader(stream) }, writer, processor)
        manager.start(logger)

        store[key] = "done"
        store["source_link_redirects"] = processor.sourceRedirects.toString()
        store["target_link_redirects"] = processor.targetRedirects.toString()
    }
}

private object ClearLookups : FlowStep<Store> {
    override val name = "Clearing lookups"

    override fun run(store: Store, logger: ProgressLogger) {
        // Save some memory
        try {
            store.pageLookup.clear()
        } catch (ignored: UninitializedPropertyAccessException) {
        }

        try {
            store.redirectLookup.clear()
        } catch (ignored: UninitializedPropertyAccessException) {
        }
    }
}

private object MoveDatabaseStep : FlowStep<Store> {
    override val name = "Moving database to target location"

    override fun run(store: Store, logger: ProgressLogger) {
        val key = "MoveDatabaseStep"
        if (store[key] != null) {
            println("Database has already been moved, skipping")
            return
        }

        databaseProvider.closeAllConnections()
        val databaseFile = File(workingDirectory, store.linksDatabaseFile.fileName)
        Files.move(
            databaseFile.toPath(),
            Paths.get(Configuration.databasesDirectory, databaseFile.name),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
        )

        store[key] = "done"
    }
}

private object DeleteTemporaryDataStep : FlowStep<Store> {
    override val name = "Deleting temporary data"

    override fun run(store: Store, logger: ProgressLogger) {
        val skippedLanguages = DumpLanguage.values().toMutableSet()
        if (!Configuration.skipDeletingDumps)
            skippedLanguages -= store.linksDatabaseFile.language!!
        else
            println("Source Wikipedia dumps won't be deleted")

        val prefixesToKeep = skippedLanguages.map { it.getFilePrefix() }
        workingDirectory.listFiles()!!
            .filter { it.name.endsWith(".gz") }
            .filter { file -> prefixesToKeep.none { prefix -> file.name.startsWith(prefix) } }
            .forEach {
                println("Deleting temporary file $it")
                it.delete()
            }

        store.clearStorage()
    }
}

private fun getDumpFile(store: Store, name: String): File {
    val namePrefix = store.linksDatabaseFile.language!!.getFilePrefix()
    return workingDirectory.listFiles()!!
        .find { it.name.startsWith(namePrefix) && it.name.endsWith("$name.sql.gz") }!!
}
