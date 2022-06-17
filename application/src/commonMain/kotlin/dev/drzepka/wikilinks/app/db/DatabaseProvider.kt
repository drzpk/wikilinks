package dev.drzepka.wikilinks.app.db

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.use
import dev.drzepka.wikilinks.app.utils.Environment
import dev.drzepka.wikilinks.app.utils.environment
import dev.drzepka.wikilinks.db.cache.CacheDatabase
import dev.drzepka.wikilinks.db.links.LinksDatabase
import mu.KotlinLogging

@Suppress("MemberVisibilityCanBePrivate")
object DatabaseProvider {
    const val LINKS_DATABASE_NAME = "links.db"
    const val CACHE_DATABASE_NAME = "cache.db"

    private val log = KotlinLogging.logger {}

    // todo: When launching from Linux, the library is looking for the a database in the a user's home directory

    fun getLinksDatabase(createSchema: Boolean = false): LinksDatabase {
        val driver = getDbDriver(LINKS_DATABASE_NAME, true)
        if (createSchema)
            LinksDatabase.Schema.createIfNecessary(driver, "links")
        return LinksDatabase.invoke(driver)
    }

    fun getCacheDatabase(): CacheDatabase {
        val driver = getDbDriver(CACHE_DATABASE_NAME, false)
        CacheDatabase.Schema.createIfNecessary(driver, "cache")
        return CacheDatabase.invoke(driver)
    }

    private fun getDbDriver(dbName: String, disableProtection: Boolean): SqlDriver {
        val driver = getDriver(dbName)

        if (disableProtection) {
            // Optimize insert speed
            driver.exec("PRAGMA journal_mode = OFF")
            driver.exec("PRAGMA synchronous = OFF")
        }

        return driver
    }

    private fun SqlDriver.Schema.createIfNecessary(driver: SqlDriver, name: String) {
        if (driver.getVersion() == 0) {
            log.info { "Created schema $name" }
            create(driver)
            driver.setVersion(1)
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
        val cursor = executeQuery(null, "PRAGMA user_version;", 0, null)
        return cursor.use { it.getLong(0)!!.toInt() }
    }

    private fun SqlDriver.setVersion(version: Int) {
        execute(null, "PRAGMA user_version = $version;", 0, null)
    }
}

internal expect fun getDriver(databaseName: String): SqlDriver
