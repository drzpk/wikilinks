package dev.drzepka.wikilinks.common.model.database

enum class DatabaseType(val languageSpecific: Boolean, val versioned: Boolean) {
    LINKS(true, true),
    CACHE(true, true),
    HISTORY(false, false)
}
