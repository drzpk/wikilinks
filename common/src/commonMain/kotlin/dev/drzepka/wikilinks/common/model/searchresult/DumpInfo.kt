package dev.drzepka.wikilinks.common.model.searchresult

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

@kotlinx.serialization.Serializable
data class DumpInfo(val language: DumpLanguage, val version: String)