package dev.drzepka.wikilinks.app.db

import com.squareup.sqldelight.db.SqlDriver
import dev.drzepka.wikilinks.app.Environment
import dev.drzepka.wikilinks.app.environment
import dev.drzepka.wikilinks.db.links.LinksDatabase

object DatabaseProvider {
    const val LINKS_DATABASE_NAME = "links.db"

    // todo: When launching from Linux, the library is looking for the a database in the a user's home directory

    fun getLinksDatabase(): LinksDatabase {
        val driver = getDbDriver(LINKS_DATABASE_NAME, true)
        return LinksDatabase.invoke(driver)
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

    private fun SqlDriver.exec(sql: String) {
        // The SQLite library on Linux doesn't support 'execute' statements.
        if (environment == Environment.LINUX)
            executeQuery(null, sql, 0)
        else
            execute(null, sql, 0)
    }
}

internal expect fun getDriver(databaseName: String): SqlDriver
