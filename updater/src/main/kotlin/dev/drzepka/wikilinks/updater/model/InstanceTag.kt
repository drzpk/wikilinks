package dev.drzepka.wikilinks.updater.model

enum class InstanceTag(val text: String) {
    LOCATOR("GeneratorRunner"),
    CREATED_AT("CreatedAt"),
    DUMP_VERSION("DumpVersion")
}
