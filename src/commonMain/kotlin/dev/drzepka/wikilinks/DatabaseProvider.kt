package dev.drzepka.wikilinks

import com.squareup.sqldelight.db.SqlDriver
import dev.drzepka.wikilinks.db.Database

object DatabaseProvider {
    fun getDatabase(): Database = Database.invoke(getDriver("database.db"))
}

internal expect fun getDriver(databaseName: String): SqlDriver
