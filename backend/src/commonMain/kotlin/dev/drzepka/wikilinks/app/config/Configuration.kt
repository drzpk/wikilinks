package dev.drzepka.wikilinks.app.config

import dev.drzepka.wikilinks.common.WikiConfig

object Configuration : BaseConfiguration() {
    val databasesDirectory by lazy { getString("DATABASES_DIRECTORY") }
    val frontendResourcesDirectory by lazy { getString("FRONTEND_RESOURCES_DIRECTORY", required = true)!! }
    val wikipediaActionApiUrl by lazy { getString("WIKIPEDIA_ACTION_API_URL", default = WikiConfig.ACTION_API_URL)!! }
}
