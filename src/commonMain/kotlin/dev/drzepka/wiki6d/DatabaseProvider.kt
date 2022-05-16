package dev.drzepka.wiki6d

import com.squareup.sqldelight.db.SqlDriver
import dev.drzepka.wiki6d.db.Database

object DatabaseProvider {
    fun getDatabase(): Database = Database.invoke(getDriver("database.db"))
}

internal expect fun getDriver(databaseName: String): SqlDriver
