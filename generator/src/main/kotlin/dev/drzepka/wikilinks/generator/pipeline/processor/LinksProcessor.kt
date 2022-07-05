package dev.drzepka.wikilinks.generator.pipeline.processor

import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.pagelookup.PageLookup

class LinksProcessor(private val pageLookup: PageLookup) : Processor<Value> {
    override fun process(value: Value): Value? {
        if (value[1] != 0) // namespace
            return null

        val sourceLink = value[0] as Int
        if (!pageLookup.hasId(sourceLink))
            return null

        val targetPage = value[2] as String
        val targetLink = pageLookup.getId(targetPage)

        return if (targetLink != null)
            listOf(sourceLink, targetLink)
        else null
    }
}
