package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.db.DatabaseProvider

fun commonMain() {
    val db = DatabaseProvider.getDatabase()
    db.linksQueries.insert(1, "a", "b")
}