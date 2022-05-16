package dev.drzepka.wiki6d

fun commonMain() {
    val db = DatabaseProvider.getDatabase()
    db.linksQueries.insert("a", "b")
}