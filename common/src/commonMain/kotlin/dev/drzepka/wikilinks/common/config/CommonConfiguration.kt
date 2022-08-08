package dev.drzepka.wikilinks.common.config

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlin.time.Duration.Companion.seconds

object CommonConfiguration : BaseConfiguration() {
    val databasesDirectory by lazy { getString("DATABASES_DIRECTORY", required = true)!! }
    val frontendResourcesDirectory by lazy { getString("FRONTEND_RESOURCES_DIRECTORY", required = true)!! }

    /**
     * Maximum amount of time to wait before closing database connection after detecting new Links database version.
     * This allows most of the database queries to complete before the server becomes unavailable.
     **/
    val databaseDisconnectTimeout = 30.seconds

    fun wikipediaActionApiUrl(language: DumpLanguage): String =
        getString("WIKIPEDIA_ACTION_API_URL_${language.name}")
            ?: "https://${language.value}.wikipedia.org/w/api.php"

    fun wikipediaRestApiUrl(language: DumpLanguage): String =
        getString("WIKIPEDIA_REST_API_URL_${language.name}")
            ?: "https://${language.value}.wikipedia.org/w/rest.php"
}
