package dev.drzepka.wikilinks.app.config

object Configuration : BaseConfiguration() {
    val databasesDirectory = getString("DATABASES_DIRECTORY")
    val frontendResourcesDirectory = getString("FRONTEND_RESOURCES_DIRECTORY", required = true)
}
