package dev.drzepka.wikilinks.generator.step

import dev.drzepka.wikilinks.app.db.DatabaseProvider
import dev.drzepka.wikilinks.app.db.FileConfigRepository
import dev.drzepka.wikilinks.generator.DatabaseSwapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Files

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DatabaseSwapperTest {

    private val tmpDirectoryPrefix = "wikilinks-swap-"
    private val databasesDirectoryName = "databases"
    private val dumpsDirectoryName = "dumps"
    private var createdTmpDirectories = mutableListOf<File>()

    @Test
    fun `should swap databases`() {
        val testDir = initializeTestDirectory()
        val step = createStep(testDir)

        step.run("1.2.3")

        val linksDbFile = File(testDir, "$databasesDirectoryName/${DatabaseProvider.LINKS_DATABASE_NAME}")
        assertEquals("new database", linksDbFile.readText())

        val versionFile = File(testDir, "$databasesDirectoryName/dump_version.txt")
        assertEquals("1.2.3", versionFile.readText())
    }

    @AfterAll
    fun cleanUp() {
        for (directory in createdTmpDirectories) {
            directory.deleteRecursively()
        }
    }

    private fun createStep(testDirectory: File): DatabaseSwapper {
        val dumpDir = File(testDirectory, dumpsDirectoryName)
        val databasesDir = File(testDirectory, databasesDirectoryName)
        val repository = FileConfigRepository(databasesDir.absolutePath)
        return DatabaseSwapper(dumpDir, databasesDir, repository)
    }

    private fun initializeTestDirectory(): File {
        val tmpDir = Files.createTempDirectory(tmpDirectoryPrefix).toFile()
        createdTmpDirectories.add(tmpDir)

        val databasesDir = File(tmpDir, databasesDirectoryName)
        databasesDir.mkdir()

        createNewFiles(
            databasesDir,
            DatabaseProvider.LINKS_DATABASE_NAME,
            "${DatabaseProvider.LINKS_DATABASE_NAME}-shm",
            "${DatabaseProvider.LINKS_DATABASE_NAME}-wal",
            DatabaseProvider.CACHE_DATABASE_NAME,
            "${DatabaseProvider.CACHE_DATABASE_NAME}-shm",
            "${DatabaseProvider.CACHE_DATABASE_NAME}-wal",
        )

        val dumpsDir = File(tmpDir, dumpsDirectoryName)
        dumpsDir.mkdir()

        val newLinksDbFile = File(dumpsDir, DatabaseProvider.LINKS_DATABASE_NAME)
        newLinksDbFile.writer().use {
            it.write("new database")
        }

        return tmpDir
    }

    private fun createNewFiles(file: File, vararg names: String) {
        for (name in names) {
            val newFile = File(file, name)
            newFile.createNewFile()
        }
    }
}
