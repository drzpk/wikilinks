package dev.drzepka.wikilinks.generator.model

import com.google.common.collect.BiMap
import dev.drzepka.wikilinks.db.links.LinksDatabase
import dev.drzepka.wikilinks.generator.pipeline.pagelookup.PageLookup

class Store {
    lateinit var db: LinksDatabase
    lateinit var pageLookup: PageLookup
    lateinit var version: String
}
