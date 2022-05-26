package dev.drzepka.wikilinks.generator.pipeline.filter

import com.google.common.collect.BiMap
import dev.drzepka.wikilinks.generator.model.Value

class LinksFilter(private val pages: BiMap<Int, String>) : Filter<Value> {
    override fun filter(value: Value): Boolean =
        value[1] == 0 // namespace
                && value[0] in pages // source link
                && value[2] in pages.inverse() // target page name
}
