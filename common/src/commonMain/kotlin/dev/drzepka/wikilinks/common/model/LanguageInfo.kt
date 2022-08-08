package dev.drzepka.wikilinks.common.model

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

@kotlinx.serialization.Serializable
data class LanguageInfo(val language: DumpLanguage, val version: String)
