package dev.drzepka.wikilinks.generator.flow

interface FlowStorage {
    operator fun get(key: String): String?
    operator fun set(key: String, value: String)

    fun clearStorage()
}
