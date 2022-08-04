package dev.drzepka.wikilinks.common.model

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlinx.serialization.Serializable

@Serializable
data class LinkSearchRequest(val language: DumpLanguage = DumpLanguage.EN, val source: Int, val target: Int)
