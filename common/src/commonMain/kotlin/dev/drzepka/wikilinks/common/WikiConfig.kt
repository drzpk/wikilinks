package dev.drzepka.wikilinks.common

object WikiConfig {
    /**
     * User agent set in compliance with the MediaWiki [API Etiquette](https://www.mediawiki.org/wiki/API:Etiquette).
     */
    const val USER_AGENT_HEADER_VALUE = "WikiLinks/${BuildConfig.VERSION} (dominik.1.rzepka@gmail.com)"
    const val USER_AGENT_HEADER = "Api-User-Agent"

    const val DUMP_SOURCE_PREFIX = "https://dumps.wikimedia.org"
    val REQUIRED_FILE_VARIANTS = listOf("page", "pagelinks", "redirect")
}
