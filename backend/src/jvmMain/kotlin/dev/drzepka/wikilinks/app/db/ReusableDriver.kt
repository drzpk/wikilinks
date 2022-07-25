package dev.drzepka.wikilinks.app.db

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcDriver

actual fun nativeClose(driver: SqlDriver): Boolean {
    if (driver !is JdbcDriver)
        return false

    driver.connectionAndClose().second()
    return true
}
