package dev.drzepka.wikilinks.common.model.database

enum class DatabaseType(val languageSpecific: Boolean, val versioned: Boolean) {
    LINKS(true, true),
    CACHE(true, false),
    HISTORY(false, false)
}
