package dev.drzepka.wikilinks.app.db

import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.use
import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.app.utils.Environment
import dev.drzepka.wikilinks.app.utils.MultiplatformWeakReference
import dev.drzepka.wikilinks.app.utils.environment
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
            }
            else {
                iterator.remove()
            }
        }

        log.info { "Closed $closedCount database connections" }
    }

    fun getLinksDatabase(
        createSchema: Boolean = false,
        disableProtection: Boolean = false,
        overrideDirectory: String? = null
    ): LinksDatabase {
        val driver = getDbDriver(LINKS_DATABASE_NAME, disableProtection, overrideDirectory)
        if (createSchema)
            LinksDatabase.Schema.createIfNecessary(driver, "links")
        return LinksDatabase.invoke(driver)
    }

    fun getCacheDatabase(): CacheDatabase {
        val driver = getDbDriver(CACHE_DATABASE_NAME, false)
        CacheDatabase.Schema.createIfNecessary(driver, "cache")
        return CacheDatabase.invoke(driver)
    }

    fun getHistoryDatabase(): HistoryDatabase {
        val driver = getDbDriver(HISTORY_DATABASE_NAME, false)
        HistoryDatabase.Schema.createIfNecessary(driver, "history")
        return HistoryDatabase.invoke(driver)
    }

    private fun getDbDriver(dbName: String, disableProtection: Boolean, overrideDirectory: String? = null): SqlDriver {
        val factory = { doGetDbDriver(dbName, disableProtection, overrideDirectory) }
        val driver = ReusableDriver(factory)
        drivers.add(MultiplatformWeakReference(driver))
        return driver
    }

    private fun doGetDbDriver(dbName: String, disableProtection: Boolean, overrideDirectory: String? = null): SqlDriver {
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

    private fun SqlDriver.Schema.createIfNecessary(driver: SqlDriver, name: String) {
        if (driver.getVersion() == 0) {
            log.info { "Creating schema $name" }
            create(driver)
            driver.setVersion(0, 1)
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
        const val LINKS_DATABASE_NAME = "links.db"
        const val CACHE_DATABASE_NAME = "cache.db"
        const val HISTORY_DATABASE_NAME = "history.db"
    }
}

internal expect fun getDriver(basePath: String?, databaseName: String): SqlDriver
