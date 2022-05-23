package dev.drzepka.wikilinks.generator.model

import dev.drzepka.wikilinks.db.Database

class Store {
    lateinit var db: Database
    lateinit var pages: Map<String, Int>
}