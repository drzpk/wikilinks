package dev.drzepka.wiki6d

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import dev.drzepka.wiki6d.db.Database

internal actual fun getDriver(databaseName: String): SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databaseName").apply {
    Database.Schema.create(this)
}