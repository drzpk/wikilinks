package dev.drzepka.wikilinks.app.db

import com.squareup.sqldelight.db.SqlDriver

actual fun nativeClose(driver: SqlDriver): Boolean = false
