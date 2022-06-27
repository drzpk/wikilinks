package dev.drzepka.wikilinks.app.config

object Configuration : BaseConfiguration() {
    val databasesDirectory = getString("DATABASES_DIRECTORY")
}
