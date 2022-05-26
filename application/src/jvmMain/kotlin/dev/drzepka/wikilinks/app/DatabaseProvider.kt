package dev.drzepka.wikilinks.app

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

internal actual fun getDriver(databaseName: String): SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databaseName").apply {
    //Database.Schema.create(this)
}