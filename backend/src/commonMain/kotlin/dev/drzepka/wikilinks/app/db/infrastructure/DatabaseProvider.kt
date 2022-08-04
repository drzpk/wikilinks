package dev.drzepka.wikilinks.app.db.infrastructure

import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.use
import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.app.utils.Environment
import dev.drzepka.wikilinks.app.utils.environment
import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.db.cache.CacheDatabase
import dev.drzepka.wikilinks.db.history.HistoryDatabase
import dev.drzepka.wikilinks.db.links.LinksDatabase
import mu.KotlinLogging

@Suppress("MemberVisibilityCanBePrivate")
class DatabaseProvider {
    private val log = KotlinLogging.logger {}

    fun getOrCreateUnprotectedLinksDatabase(file: DatabaseFile, directory: String): LinksDatabase {
        file.verifyType(DatabaseType.LINKS)

        val driver = getDbDriver(file.fileName, true, directory)
        LinksDatabase.Schema.createOrMigrateIfNecessary(driver, "links", LINKS_DATABASE_VERSION)
        return LinksDatabase.invoke(driver)
    }

    fun getLinksDatabase(file: DatabaseFile): ManagedDatabase<LinksDatabase> {
        file.verifyType(DatabaseType.LINKS)

        val driver = getDbDriver(file.fileName, false, null)
        val database = LinksDatabase.invoke(driver)
        return ManagedDatabase(database, driver, file)
    }

    fun getOrCreateCacheDatabase(file: DatabaseFile): ManagedDatabase<CacheDatabase> {
        file.verifyType(DatabaseType.CACHE)

        val driver = getDbDriver(file.fileName, false)
        CacheDatabase.Schema.createOrMigrateIfNecessary(driver, "cache", CACHE_DATABASE_VERSION)

        val database = CacheDatabase.invoke(driver)
        return ManagedDatabase(database, driver, file)
    }

    fun getOrCreateHistoryDatabase(file: DatabaseFile): ManagedDatabase<HistoryDatabase> {
        file.verifyType(DatabaseType.HISTORY)

        val driver = getDbDriver(file.fileName, false)
        HistoryDatabase.Schema.createOrMigrateIfNecessary(driver, "history", HISTORY_DATABASE_VERSION)

        val database = HistoryDatabase.invoke(driver)
        return ManagedDatabase(database, driver, file)
    }

    private fun DatabaseFile.verifyType(expected: DatabaseType) {
        if (type != expected)
            throw IllegalStateException("Expected DatabaseFile with type $expected, but got $type instead")
    }

    private fun getDbDriver(dbName: String, disableProtection: Boolean, overrideDirectory: String? = null): SqlDriver {
        val driver = doGetDbDriver(dbName, disableProtection, overrideDirectory)
        return CloseableDriver(driver)
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
