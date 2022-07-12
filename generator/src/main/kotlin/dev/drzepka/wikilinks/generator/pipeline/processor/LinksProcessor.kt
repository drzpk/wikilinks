package dev.drzepka.wikilinks.generator.pipeline.processor

import dev.drzepka.wikilinks.generator.model.Value
import dev.drzepka.wikilinks.generator.pipeline.lookup.PageLookup
import dev.drzepka.wikilinks.generator.pipeline.lookup.RedirectLookup

class LinksProcessor(private val pageLookup: PageLookup, private val redirectLookup: RedirectLookup) :
    Processor<Value> {

    var sourceRedirects = 0
        private set
    var targetRedirects = 0
        private set

    override fun process(value: Value): Value? {
        if (value[1] != 0) // namespace
            return null

        // Optimization: look by page title first (see InMemoryPageLookup for more info).
        // Performance gain: 0.6 percentage point of progress in 36 seconds (step: page link dump extraction).
        // Total step time reduction: from 17 to 14.7 minutes (13.5%)
        val targetPage = value[2] as String
        val targetLink = pageLookup.getId(targetPage) ?: return null
        val sourceLink = value[0] as Int

        val redirectedSource = redirectLookup[sourceLink] ?: sourceLink
        val redirectedTarget = redirectLookup[targetLink] ?: targetLink

        if (sourceLink != redirectedSource)
            sourceRedirects++
        if (targetLink != redirectedTarget)
            targetRedirects++

        return if (pageLookup.hasId(redirectedSource) && pageLookup.hasId(redirectedTarget))
            return listOf(redirectedSource, redirectedTarget)
        else null
    }
}
