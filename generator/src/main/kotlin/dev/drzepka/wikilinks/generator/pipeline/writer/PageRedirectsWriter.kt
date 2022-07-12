package dev.drzepka.wikilinks.generator.pipeline.writer

import dev.drzepka.wikilinks.db.links.LinksDatabase
import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.lookup.PageLookup
import dev.drzepka.wikilinks.generator.pipeline.lookup.RedirectLookup

class PageRedirectsWriter(
    private val pageLookup: PageLookup,
    private val redirectLookup: RedirectLookup,
    db: LinksDatabase
) : AbstractWriter<Value>(db, 1_000_000) {

    override fun filter(value: Value): Boolean {
        // Only store pages with namespace == 0
        return value[1] == 0
    }

    override fun insert(value: List<Any?>) {
        val from = value[0] as Int
        val to = pageLookup.getId(value[2] as String) ?: return

        redirectLookup[from] = to
        db.pagesQueries.setRedirect(from.toLong(), to.toLong())
    }
}
