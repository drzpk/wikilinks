package dev.drzepka.wikilinks.common.model.dump

import dev.drzepka.wikilinks.common.WikiConfig

enum class DumpLanguage {
    EN, PL;

    fun getSourceUrl(): String = "${WikiConfig.DUMP_SOURCE_PREFIX}/${name.lowercase()}wiki"

    fun getFilePrefix(): String = name.lowercase() + "wiki-"

    fun getRestApiUrl(): String = "https://${name.lowercase()}.wikipedia.org/w/rest.php"
    fun getActionApiUrl(): String = "https://${name.lowercase()}.wikipedia.org/w/api.php"

    companion object {
        fun fromString(raw: String): DumpLanguage? = try {
            valueOf(raw.uppercase())
        } catch (e: Exception) {
            null
        }
    }
}
