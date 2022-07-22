package dev.drzepka.wikilinks.app.model

import dev.drzepka.wikilinks.common.model.searchresult.PageInfo

data class PageInfoResult(val pages: Map<Int, PageInfo>, val cacheHitRatio: Float, val pageFetchMs: Int)
