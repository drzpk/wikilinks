package dev.drzepka.wikilinks.app.db.infrastructure

import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.use
import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.app.utils.Environment
import dev.drzepka.wikilinks.app.utils.MultiplatformWeakReference
import dev.drzepka.wikilinks.app.utils.environment
import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.db.cache.CacheDatabase
import dev.drzepka.wikilinks.db.history.HistoryDatabase
import dev.drzepka.wikilinks.db.links.LinksDatabase
import mu.KotlinLogging

@Suppress("MemberVisibilityCanBePrivate")
class DatabaseProvider {
    private val log = KotlinLogging.logger {}
    private val drivers = mutableSetOf<MultiplatformWeakReference<SqlDriver>>()

    fun closeAllConnections() {
        val iterator = drivers.iterator()
        var closedCount = 0

        while (iterator.hasNext()) {
            val driver = iterator.next().getValue()
            if (driver != null) {
                driver.close()
                closedCount++
            } else {
                iterator.remove()
            }
        }

        log.info { "Closed $closedCount database connections" }
    }

    fun getLinksDatabase(
        language: DumpLanguage,
        fixedVersion: String? = null,
        disableProtection: Boolean = false,
        overrideDirectory: String? = null
    ): LinksDatabase {
        val nameResolver = { resolveDatabaseName(DatabaseType.LINKS, language, fixedVersion, overrideDirectory) }
        val initializer = { driver: SqlDriver ->
            LinksDatabase.Schema.createOrMigrateIfNecessary(driver, "links", LINKS_DATABASE_VERSION)
        }

        val driver = getDbDriver(nameResolver, initializer, disableProtection, overrideDirectory)
        return LinksDatabase.invoke(driver)
    }

    fun getCacheDatabase(language: DumpLanguage): CacheDatabase {
        val nameResolver = { resolveDatabaseName(DatabaseType.CACHE, language) }
        val initializer = { driver: SqlDriver ->
            CacheDatabase.Schema.createOrMigrateIfNecessary(driver, "cache", CACHE_DATABASE_VERSION)
        }

        val driver = getDbDriver(nameResolver, initializer, false)
        return CacheDatabase.invoke(driver)
    }

    fun getHistoryDatabase(): HistoryDatabase {
        val nameResolver = { resolveDatabaseName(DatabaseType.HISTORY) }
        val initializer = { driver: SqlDriver ->
            HistoryDatabase.Schema.createOrMigrateIfNecessary(driver, "history", HISTORY_DATABASE_VERSION)
        }

        val driver = getDbDriver(nameResolver, initializer, false)
        return HistoryDatabase.invoke(driver)
    }

    private fun resolveDatabaseName(
        type: DatabaseType,
        language: DumpLanguage? = null,
        fixedVersion: String? = null,
        overrideDirectory: String? = null
    ): String {
        val dir = overrideDirectory ?: Configuration.databasesDirectory!!

        return if (type.versioned) {
            if (fixedVersion != null) {
                DatabaseFile.create(type, language = language, version = fixedVersion).fileName
            } else {
                DatabaseResolver.resolveDatabaseName(dir, type, language)
                    ?: throw IllegalStateException("Database of type $type doesn't exist")
            }
        } else {
            DatabaseFile.create(type, language).fileName
        }
    }

    private fun getDbDriver(
        dbNameResolver: () -> String,
        postInitializationHandler: (driver: SqlDriver) -> Unit,
        disableProtection: Boolean,
        overrideDirectory: String? = null
    ): SqlDriver {
        val factory = {
            val dbName = dbNameResolver()
            val driver = doGetDbDriver(dbName, disableProtection, overrideDirectory)
            postInitializationHandler(driver)
            driver
        }

        val driver = ReusableDriver(factory)
        drivers.add(MultiplatformWeakReference(driver))
        return driver
    }

    private fun doGetDbDriver(
        dbName: String,
        disableProtection: Boolean,
        overrideDirectory: String? = null
    ): SqlDriver {
        val dir = overrideDirectory ?: Configuration.databasesDirectory
        val driver = getDriver(dir, dbName)

        // Optimize insert speed
        // https://www.sqlite.org/pragma.html
        val journalMode = if (disableProtection) "OFF" else "DELETE"
        val synchronous = if (disableProtection) "OFF" else "FULL"

        driver.exec("PRAGMA journal_mode = $journalMode")
        driver.exec("PRAGMA synchronous = $synchronous")

        return driver
    }

    private fun SqlDriver.Schema.createOrMigrateIfNecessary(driver: SqlDriver, name: String, currentVersion: Int) {
        val schemaVersion = driver.getVersion()
        if (schemaVersion == 0) {
            log.info { "Creating schema $name" }
            create(driver)
            driver.setVersion(0, 1)
        } else if (schemaVersion < currentVersion) {
            log.info { "Migrating schema $name from $schemaVersion to $currentVersion" }
            migrate(driver, schemaVersion, currentVersion)
            driver.setVersion(schemaVersion, currentVersion)
        }
    }

    private fun SqlDriver.exec(sql: String) {
        // The SQLite library on Linux doesn't support 'execute' statements.
        if (environment == Environment.LINUX)
            executeQuery(null, sql, 0)
        else
            execute(null, sql, 0)
    }

    private fun SqlDriver.getVersion(): Int {
        // Sqlite also supports PRAGMA user_version, but for some reason it doesn't work on native driver.
        val tableExists = executeQuery("SELECT name from sqlite_master WHERE type = 'table' and name = 'Version'")
            .use { it.next() && it.getString(0) != null }

        if (!tableExists)
            return 0

        return executeQuery("SELECT version FROM Version").use { it.next(); it.getLong(0) }!!.toInt()
    }

    private fun SqlDriver.setVersion(oldVersion: Int, newVersion: Int) {
        if (oldVersion == 0) {
            execute(null, "CREATE TABLE Version (version INTEGER NOT NULL);", 0)
            execute(null, "INSERT INTO Version VALUES ($newVersion);", 0)
        } else {
            execute(null, "UPDATE Version SET version = $newVersion;", 0)
        }
    }

    private fun SqlDriver.executeQuery(sql: String): SqlCursor = executeQuery(null, sql, 0)

    companion object {
        private const val LINKS_DATABASE_VERSION = 1
        private const val CACHE_DATABASE_VERSION = 1
        private const val HISTORY_DATABASE_VERSION = 2
    }
}

internal expect fun getDriver(basePath: String?, databaseName: String): SqlDriver
