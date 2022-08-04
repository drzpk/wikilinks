package dev.drzepka.wikilinks.app.db.infrastructure

import dev.drzepka.wikilinks.app.LanguageNotAvailableException
import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.db.cache.CacheDatabase
import dev.drzepka.wikilinks.db.history.HistoryDatabase
import dev.drzepka.wikilinks.db.links.LinksDatabase
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import mu.KotlinLogging
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DatabaseRegistry {
    private val log = KotlinLogging.logger {}
    private val provider = DatabaseProvider()

    private val canReadFromLanguageGroups = atomic(true)
    private val languageGroups = mutableMapOf<DumpLanguage, DatabaseGroup>()
    private lateinit var historyDatabase: ManagedDatabase<HistoryDatabase>

    init {
        loadDatabases()
    }

    suspend fun getAvailableLanguages(): Map<DumpLanguage, String> {
        waitForReading()
        return languageGroups
            .values
            .map { it.links.databaseFile }
            .associate { it.language!! to it.version!! }

    }

    suspend fun getLinksDatabase(language: DumpLanguage): LinksDatabase {
        waitForReading()
        val group = languageGroups.getForLanguage(language)
        return group.links.database
    }

    suspend fun getCacheDatabase(language: DumpLanguage): CacheDatabase {
        waitForReading()
        val group = languageGroups.getForLanguage(language)
        return group.cache.database
    }

    fun getHistoryDatabase(): HistoryDatabase = historyDatabase.database

    suspend fun unregisterLanguage(language: DumpLanguage) {
        lockLanguageGroups {
            languageGroups.remove(language)?.close()
        }
    }

    suspend fun updateDatabases(language: DumpLanguage) {
        lockLanguageGroups {
            doUpdateDatabase(language)
        }
    }

    private suspend fun lockLanguageGroups(block: suspend () -> Unit) {
        // A simple variable is used instead of locks, should be faster this way.
        // The delay is to ensure that all operations on the language groups have ceased.
        canReadFromLanguageGroups.value = false
        delay(500)

        try {
            block()
        } finally {
            canReadFromLanguageGroups.value = true
        }
    }

    private suspend fun doUpdateDatabase(language: DumpLanguage) {
        val startTime = Clock.System.now()
        log.info { "Updating databases with lang=$language" }

        val oldGroup = languageGroups[language]
        log.debug {
            val linksVersion = oldGroup?.links?.databaseFile?.version
            val cacheVersion = oldGroup?.cache?.databaseFile?.version
            "Old versions: links=$linksVersion, cache=$cacheVersion"
        }

        try {
            loadLanguageSpecificDatabase(language)
        } catch (e: Exception) {
            if (oldGroup != null)
                languageGroups[language] = oldGroup
            throw e
        }

        oldGroup?.close()

        val duration = Clock.System.now() - startTime
        log.info { "Update for lang=$language has finished. The process took $duration" }
    }

    private suspend fun waitForReading() {
        while (!canReadFromLanguageGroups.value)
            delay(100)
    }

    private fun Map<DumpLanguage, DatabaseGroup>.getForLanguage(language: DumpLanguage): DatabaseGroup =
        this[language] ?: throw LanguageNotAvailableException(language)

    private fun loadDatabases() {
        historyDatabase = provider.getOrCreateHistoryDatabase(DatabaseFile.create(DatabaseType.HISTORY))
        loadLanguageSpecificDatabases()
    }

    private fun loadLanguageSpecificDatabases() {
        for (language in DumpLanguage.values()) {
            loadLanguageSpecificDatabase(language)
        }
    }

    private fun loadLanguageSpecificDatabase(language: DumpLanguage) {
        val linksFile = DatabaseResolver.resolveNewestDatabaseFile(DatabaseType.LINKS, language)
        if (linksFile == null) {
            log.debug { "No Links database available for lang=$language" }
            return
        }

        log.info { "Loading databases for lang=$language" }
        val cacheFile = DatabaseFile.create(DatabaseType.CACHE, language, linksFile.version!!)
        val links = provider.getLinksDatabase(linksFile)
        val cache = provider.getOrCreateCacheDatabase(cacheFile)

        languageGroups[language] = DatabaseGroup(links, cache)
    }

    private data class DatabaseGroup(
        val links: ManagedDatabase<LinksDatabase>,
        val cache: ManagedDatabase<CacheDatabase>
    ) {
        suspend fun close() {
            // This solution is not ideal, because there may be cases when more time is required
            // to finish all queries that the hardcoded values. But it should be sufficient
            // most of the time. An edge case-free solution would probably require tracking
            // and waiting for all ongoing requests before releasing database connections.
            log.info { "Waiting ${Configuration.databaseDisconnectTimeout} before closing old databases" }
            delay(Configuration.databaseDisconnectTimeout)

            log.debug { "Closing old database connections" }

            links.close()
            cache.close()
        }

        companion object {
            private val log = KotlinLogging.logger {}
        }
    }
}
