package dev.drzepka.wikilinks

import com.squareup.sqldelight.db.SqlDriver
import dev.drzepka.wikilinks.db.Database

object DatabaseProvider {
    val databaseName = "database.db"
    fun getDatabase(): Database {
        val driver = getDriver(databaseName)

        // Optimize insert speed
        driver.execute(null, "PRAGMA journal_mode = MEMORY", 0)
        driver.execute(null, "PRAGMA synchronous = OFF", 0)

        return Database.invoke(driver)
    }
}

internal expect fun getDriver(databaseName: String): SqlDriver
