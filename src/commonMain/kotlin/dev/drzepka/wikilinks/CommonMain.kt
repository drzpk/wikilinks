package dev.drzepka.wikilinks

fun commonMain() {
    val db = DatabaseProvider.getDatabase()
    db.linksQueries.insert("a", "b")
}