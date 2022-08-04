package dev.drzepka.wikilinks.app.db.infrastructure

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDriver
import dev.drzepka.wikilinks.common.model.database.DatabaseFile

class ManagedDatabase<T : Transacter>(
    val database: T,
    private val driver: SqlDriver,
    val databaseFile: DatabaseFile
) {
    fun close() = driver.close()
}
