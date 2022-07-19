package dev.drzepka.wikilinks.generator

import dev.drzepka.wikilinks.app.db.ConfigRepository
import dev.drzepka.wikilinks.app.db.DatabaseProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DatabaseSwapper(
    private val dumpDirectory: File,
    private val databasesDirectory: File,
    private val configRepository: ConfigRepository,
    private val maintenanceLockWaitTime: Duration = Configuration.databaseDisconnectTimeout
) {
    private val oldDatabasesDirectory = File(databasesDirectory, "_old")

    fun run(newVersion: String) {
        ensureDatabasesDirectoryExists()
        activateMaintenanceLock()

        try {
            moveOldDatabases()
            moveNewLinksDatabase()
            updateVersion(newVersion)
            deleteOldDatabases()
        } catch (e: Exception) {
            try {
                restoreOldDatabases()
            } catch (e: Exception) {
                println("Error while restoring old databases")
                e.printStackTrace()
            }

            throw e
        } finally {
            deactivateMaintenanceLock()
        }
    }

    private fun ensureDatabasesDirectoryExists() {
        if (!databasesDirectory.isDirectory) {
            println("Databases directory doesn't exist, creating")
            databasesDirectory.mkdir()
        }
    }

    private fun activateMaintenanceLock() {
        configRepository.setMaintenanceMode(true)
        println("Waiting $maintenanceLockWaitTime for the application to detect maintenance mode")
        Thread.sleep(maintenanceLockWaitTime.inWholeMilliseconds)
    }

    private fun moveOldDatabases() {
        val knownDatabases = listOf(DatabaseProvider.LINKS_DATABASE_NAME, DatabaseProvider.CACHE_DATABASE_NAME)
        val databases = databasesDirectory
            .listFiles()!!
            .filter { knownDatabases.any { k -> it.name.startsWith(k) } && !it.name.endsWith(".old.db") }

        oldDatabasesDirectory.mkdir()

        val waitTime = 30.seconds
        for (database in databases) {
            println("Moving database: ${database.name}")
            tryToMoveFile(database, oldDatabasesDirectory, waitTime)
        }
    }

    private fun restoreOldDatabases() {
        print("Restoring old databases:")
        oldDatabasesDirectory.listFiles()!!.forEach {
            print(" - ${it.name}")
            tryToMoveFile(it, databasesDirectory, 10.seconds)
        }
    }

    private fun moveNewLinksDatabase() {
        val databaseFile = File(dumpDirectory, DatabaseProvider.LINKS_DATABASE_NAME)
        if (!databaseFile.isFile)
            throw IllegalStateException("Links database file wasn't found at: $databaseFile")

        tryToMoveFile(databaseFile, databasesDirectory, 10.seconds)
    }

    private fun updateVersion(newVersion: String) {
        configRepository.setDumpVersion(newVersion)
    }

    private fun deleteOldDatabases() {
        deleteMatchingFiles(oldDatabasesDirectory) { true }
    }

    private fun deactivateMaintenanceLock() {
        configRepository.setMaintenanceMode(false)
    }

    private fun tryToMoveFile(file: File, targetDirectory: File, timeout: Duration) {
        val targetFile = targetDirectory.toPath().resolve(file.name)
        val endTime = Instant.now().plus(timeout.inWholeMilliseconds, ChronoUnit.MILLIS)

        var attempt = 0
        var moved = false
        while (Instant.now().isBefore(endTime)) {
            attempt++

            try {
                Files.move(
                    file.toPath(), targetFile,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
                )
                moved = true
                break
            } catch (e: Exception) {
                println("Move attempt $attempt failed (${e.message})")
                Thread.sleep(5000)
            }
        }

        if (!moved)
            throw IllegalStateException("File moving failed: $file")

        if (attempt > 1)
            println("Moved successfully after $attempt attempt(s)")
    }

    private fun deleteMatchingFiles(directory: File, matcher: (String) -> Boolean) {
        directory
            .listFiles()!!
            .filter { matcher.invoke(it.name) }
            .forEach {
                println("Deleting: $it")
                it.delete()
            }
    }
}
