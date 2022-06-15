package dev.drzepka.wikilinks.common.model

import kotlinx.serialization.Serializable

@Serializable
data class LinkSearchRequest(val source: Int, val target: Int)
