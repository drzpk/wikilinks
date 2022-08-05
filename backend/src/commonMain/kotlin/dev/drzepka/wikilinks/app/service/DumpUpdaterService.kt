package dev.drzepka.wikilinks.app.service

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseResolver
import dev.drzepka.wikilinks.common.config.CommonConfiguration
import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.utils.MultiplatformDirectory
import dev.drzepka.wikilinks.common.utils.MultiplatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DumpUpdaterService(scope: CoroutineScope, private val databaseRegistry: DatabaseRegistry) {
    private val log = KotlinLogging.logger {}

    init {
        scope.launch {
            while (isActive) {
                checkForDatabaseUpdate()
            }
        }
    }

    private suspend fun checkForDatabaseUpdate() {
        delay(30.seconds)
        val files = DatabaseResolver.resolveDatabaseFiles()
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

    private suspend fun updateDatabase(files: List<DatabaseFile>) {
        val filesByLanguage = files.groupBy { it.language!! }
        val currentlyUsedLanguages = databaseRegistry.getAvailableLanguages()

        for (group in filesByLanguage) {
            updateDatabase(group.key, group.value, currentlyUsedLanguages[group.key])
        }
    }

    private suspend fun updateDatabase(language: DumpLanguage, files: List<DatabaseFile>, currentVersion: String?) {
        val newLinksDatabase = files
            .filter { it.type == DatabaseType.LINKS }
            .getMostRecentVersion()

        if (newLinksDatabase == null && currentVersion != null) {
            log.warn { "Links database for lang=$language went offline, unregistering" }
            databaseRegistry.unregisterLanguage(language)
            return
        } else if (newLinksDatabase != null && newLinksDatabase.version == currentVersion) {
            log.trace { "Links database for lang=$language is up to date ($currentVersion)" }
            return
        } else if (newLinksDatabase == null && currentVersion == null) {
            // Database doesn't exist at all
            return
        }

        newLinksDatabase as DatabaseFile
        log.info { "Detected new Links database version: ${newLinksDatabase.version}, current version: $currentVersion" }

        log.info { "Starting the update" }
        databaseRegistry.updateDatabases(language)

        log.info { "Deleting old databases" }
        deleteDatabases(files - newLinksDatabase)

        log.info { "Update complete" }
        delay(10.seconds)
    }

    private fun Iterable<DatabaseFile>.getMostRecentVersion(): DatabaseFile? =
        sortedByDescending { it.version }.firstOrNull()

    private suspend fun deleteDatabases(files: Collection<DatabaseFile>) {
        val databasesDir = MultiplatformDirectory(CommonConfiguration.databasesDirectory!!)
        val filesToDelete = files.map { it.fileName }

        databasesDir.listFiles()
            .filter {it.getName() in filesToDelete }
            .forEach {
                log.info { "Deleting database: ${it.getName()}" }
                it.tryToDelete()
            }
    }

    private suspend fun MultiplatformFile.tryToDelete() {
        var originalException: Exception? = null

        val attempts = 6
        for (i in 0 until attempts) {
            try {
                log.trace { "Deleting file ${getName()}, attempt ${i + 1}/$attempts" }
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
}
