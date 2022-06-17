package dev.drzepka.wikilinks.generator.model

import com.google.common.collect.BiMap
import dev.drzepka.wikilinks.db.links.LinksDatabase

class Store {
    lateinit var db: LinksDatabase
    lateinit var pages: BiMap<Int, String>
}