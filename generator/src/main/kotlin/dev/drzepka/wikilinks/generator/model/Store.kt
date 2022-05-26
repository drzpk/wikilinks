package dev.drzepka.wikilinks.generator.model

import com.google.common.collect.BiMap
import dev.drzepka.wikilinks.db.Database

class Store {
    lateinit var db: Database
    lateinit var pages: BiMap<Int, String>
}