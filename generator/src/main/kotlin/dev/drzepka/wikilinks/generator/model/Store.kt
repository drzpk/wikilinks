package dev.drzepka.wikilinks.generator.model

import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.db.links.LinksDatabase
import dev.drzepka.wikilinks.generator.flow.FlowStorage
import dev.drzepka.wikilinks.generator.pipeline.lookup.PageLookup
import dev.drzepka.wikilinks.generator.pipeline.lookup.RedirectLookup

class Store(storage: FlowStorage) : FlowStorage by storage {
    lateinit var db: LinksDatabase
    lateinit var pageLookup: PageLookup
    lateinit var redirectLookup: RedirectLookup
    lateinit var linksDatabaseFile: DatabaseFile
}
