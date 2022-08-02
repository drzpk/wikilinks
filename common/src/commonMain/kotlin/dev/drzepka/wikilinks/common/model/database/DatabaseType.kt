package dev.drzepka.wikilinks.common.model.database

enum class DatabaseType(val versioned: Boolean) {
    LINKS(true),
    CACHE(false),
    HISTORY(false)
}
