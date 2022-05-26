package dev.drzepka.wikilinks.app

fun commonMain() {
    val db = DatabaseProvider.getDatabase()
    db.linksQueries.insert(1, "a", "b")
}