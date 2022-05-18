package dev.drzepka.wikilinks

import com.squareup.sqldelight.db.SqlDriver
import dev.drzepka.wikilinks.db.Database

object DatabaseProvider {
    val databaseName = "database.db"
    fun getDatabase(): Database = Database.invoke(getDriver(databaseName))
}

internal expect fun getDriver(databaseName: String): SqlDriver
