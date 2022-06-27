package dev.drzepka.wikilinks.app.db

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

internal actual fun getDriver(basePath: String?, databaseName: String): SqlDriver {
    var base = basePath ?: ""
    if (base.isNotEmpty())
        base += '/'
    return JdbcSqliteDriver("jdbc:sqlite:$base$databaseName")
}
