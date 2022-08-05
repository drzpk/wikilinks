package dev.drzepka.wikilinks.common.model.dump

import dev.drzepka.wikilinks.common.WikiConfig

enum class DumpLanguage {
    EN, PL;

    val value: String
        get() = name.lowercase()

    fun getSourceUrl(): String = "${WikiConfig.DUMP_SOURCE_PREFIX}/${value}wiki"

    fun getFilePrefix(): String = "${value}wiki-"

    companion object {
        fun fromString(raw: String): DumpLanguage? = try {
            valueOf(raw.uppercase())
        } catch (e: Exception) {
            null
        }
    }
}
