package dev.drzepka.wikilinks

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import dev.drzepka.wikilinks.db.Database

internal actual fun getDriver(databaseName: String): SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databaseName").apply {
    //Database.Schema.create(this)
}