package dev.drzepka.wikilinks.front.util

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.common.model.searchresult.PageInfo

fun LinkSearchResult.sourcePage(): PageInfo? = paths.firstOrNull()?.pages?.firstOrNull()?.let { pages[it] }
fun LinkSearchResult.targetPage(): PageInfo? = paths.firstOrNull()?.pages?.lastOrNull()?.let { pages[it] }
