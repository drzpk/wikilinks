package dev.drzepka.wiki6d

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import dev.drzepka.wiki6d.db.Database

internal actual fun getDriver(databaseName: String): SqlDriver = NativeSqliteDriver(Database.Schema, databaseName)