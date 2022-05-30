package dev.drzepka.wikilinks.app.db

import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

internal actual fun getDriver(databaseName: String): SqlDriver = NativeSqliteDriver(DatabaseConfiguration(
    name = databaseName,
    version = 1,
    // Disable schema creation and migrations
    create = {},
    upgrade = { _, _, _ -> }
))
