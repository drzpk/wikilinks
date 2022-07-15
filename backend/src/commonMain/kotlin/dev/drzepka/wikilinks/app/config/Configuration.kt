package dev.drzepka.wikilinks.app.config

object Configuration : BaseConfiguration() {
    val databasesDirectory by lazy { getString("DATABASES_DIRECTORY") }
    val frontendResourcesDirectory by lazy { getString("FRONTEND_RESOURCES_DIRECTORY", required = true) }
}
