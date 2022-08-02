package dev.drzepka.wikilinks.app.config

import dev.drzepka.wikilinks.common.WikiConfig
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object Configuration : BaseConfiguration() {
    val databasesDirectory by lazy { getString("DATABASES_DIRECTORY") }
    val frontendResourcesDirectory by lazy { getString("FRONTEND_RESOURCES_DIRECTORY", required = true)!! }
    val wikipediaActionApiUrl by lazy { getString("WIKIPEDIA_ACTION_API_URL", default = WikiConfig.ACTION_API_URL)!! }

    /**
     * Maximum amount of time to wait before closing database connection after detecting new Links database version.
     * This allows most of the database queries to complete before the server becomes unavailable.
     **/
    val databaseDisconnectTimeout = 30.seconds
}
