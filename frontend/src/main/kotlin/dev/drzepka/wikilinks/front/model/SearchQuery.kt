package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

data class SearchQuery(val sourcePage: String, val targetPage: String, val language: DumpLanguage?)
