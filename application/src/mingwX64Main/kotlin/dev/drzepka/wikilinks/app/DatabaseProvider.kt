package dev.drzepka.wikilinks.app

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import dev.drzepka.wikilinks.db.Database

internal actual fun getDriver(databaseName: String): SqlDriver = NativeSqliteDriver(Database.Schema, databaseName)