package dev.drzepka.wikilinks.common

object WikiConfig {
    const val REST_API_URL = "https://en.wikipedia.org/w/rest.php"
    const val ACTION_API_URL = "https://en.wikipedia.org/w/api.php"

    // TODO: generate a class with version and use it here
    /**
     * User agent set in compliance with the MediaWiki [API Etiquette](https://www.mediawiki.org/wiki/API:Etiquette).
     */
    const val USER_AGENT_HEADER_VALUE = "WikiLinks/1.0-SNAPSHOT (dominik.1.rzepka@gmail.com)"
    const val USER_AGENT_HEADER = "Api-User-Agent"

    const val DUMP_SOURCE = "https://dumps.wikimedia.org/enwiki"
    val REQUIRED_FILE_VARIANTS = listOf("page", "pagelinks", "redirect")
}
