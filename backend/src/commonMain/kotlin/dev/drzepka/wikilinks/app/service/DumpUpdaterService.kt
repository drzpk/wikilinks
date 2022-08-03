package dev.drzepka.wikilinks.app.service

import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.app.db.DatabaseProvider
import dev.drzepka.wikilinks.app.db.DatabaseResolver
import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.utils.MultiplatformDirectory
import dev.drzepka.wikilinks.common.utils.MultiplatformFile
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DumpUpdaterService(
    scope: CoroutineScope,
    private val databaseProvider: DatabaseProvider
) {
    private val log = KotlinLogging.logger {}
    private val updateInProgress = atomic(false)
    private val databasesDir = MultiplatformDirectory(Configuration.databasesDirectory!!)

    var dumpVersion: String? = null
        private set

    init {
        refreshDumpVersion()
        scope.launch {
            while (isActive) {
                checkForDatabaseUpdate()
            }
        }
    }

    fun isUpdateInProgress(): Boolean = updateInProgress.value

    private suspend fun checkForDatabaseUpdate() {
        // This solution is not ideal, because there may be cases when more time is required
        // to finish all queries that the hardcoded values. But it should be sufficient
        // most of the time. An edge case-free solution would probably require tracking
        // and waiting for all ongoing requests before releasing database connections.

        delay(10.seconds)
        val files = databasesDir.listFiles()
        if (files.isEmpty())
            return

        try {
            updateDatabase(files)
        } catch (e: Exception) {
            val cooldown = 1.hours
            log.error(e) { "Uncaught exception occurred while updating database, pausing update checking for $cooldown" }
            delay(cooldown)
        }
    }

    private suspend fun updateDatabase(files: List<MultiplatformFile>) {
        val databaseFiles = files.mapNotNull { DatabaseFile.parse(it.getName()) }
        if (databaseFiles.isEmpty())
            return

        val linksDatabaseFiles = databaseFiles.filter { it.type == DatabaseType.LINKS }
        if (linksDatabaseFiles.size < 2)
            return

        val mostRecentDatabase = linksDatabaseFiles.getMostRecentVersion()
        log.info { "Detected new Links database version: ${mostRecentDatabase.version}, current version: $dumpVersion" }

        log.info { "Starting the update" }
        updateInProgress.value = true
        delay(Configuration.databaseDisconnectTimeout) // Wait for all ongoing requests to finish

        log.info { "Closing database connections" }
        databaseProvider.closeAllConnections()

        log.info { "Deleting old databases" }
        deleteDatabases(setOf(DatabaseType.LINKS, DatabaseType.CACHE), setOf(mostRecentDatabase.fileName))

        log.info { "Update complete" }
        updateInProgress.value = false
        refreshDumpVersion()
        delay(10.seconds)
    }

    private fun Iterable<DatabaseFile>.getMostRecentVersion(): DatabaseFile = sortedByDescending { it.version }.first()

    private suspend fun deleteDatabases(types: Set<DatabaseType>, whitelist: Set<String>) {
        val typesSet = types.toSet()
        val databasesDir = MultiplatformDirectory(Configuration.databasesDirectory!!)

        databasesDir.listFiles()
            .filter {
                val dbFile = DatabaseFile.parse(it.getName())
                dbFile != null && dbFile.type in typesSet && it.getName() !in whitelist
            }
            .forEach {
                log.info { "Deleting database: ${it.getName()}" }
                it.tryToDelete()
            }
    }

    private suspend fun MultiplatformFile.tryToDelete() {
        var originalException: Exception? = null

        for (i in 0 until 6) {
            try {
                delete()
                return
            } catch (e: Exception) {
                if (originalException == null)
                    originalException = e

                val cooldown = 10
                log.warn { "Deletion failed. Retrying in $cooldown seconds" }
                delay(cooldown * 1000L)
            }
        }

        throw IllegalStateException("Deletion of file ${getName()} failed", originalException)
    }

    private fun refreshDumpVersion() {
        val linksFile = DatabaseResolver.resolveDatabaseFile(
            Configuration.databasesDirectory!!,
            DatabaseType.LINKS,
            DumpLanguage.EN // todo
        )
        dumpVersion = linksFile?.version
    }
}
