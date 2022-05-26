package dev.drzepka.wikilinks.generator.pipeline.reader

import dev.drzepka.wikilinks.generator.model.Link
import dev.drzepka.wikilinks.generator.model.LinkGroup
import java.util.concurrent.BlockingQueue

class LinksFileReader(
    private val reader: Reader,
    private val linkQueue: BlockingQueue<LinkGroup>,
    val groupingColumn: Int
) : Runnable {

    // Use separate public variable instead endOfLinks to prevent from race conditions when
    // endOfLinks has been set to true, but payload wasn't dumped to the queue.
    var done = false
        private set

    private var endOfLinks = false
    private var firstGroupLink: Link
    private var lastGroupingColumnValue = -1
    private var nextGroupingColumnValue: Int

    init {
        if (groupingColumn !in (0..1))
            throw IllegalArgumentException("Invalid grouping column range")

        if (reader.hasNext()) {
            val line = reader.next()
            firstGroupLink = parseLink(line)
            nextGroupingColumnValue = firstGroupLink[groupingColumn]
        } else {
            firstGroupLink = Link(0, 0)
            nextGroupingColumnValue = 0
            endOfLinks = true
        }
    }

    override fun run() {
        while (!endOfLinks) {
            val links = nextGroupLinks()
            if (links.isNotEmpty()) {
                val group = LinkGroup(lastGroupingColumnValue, links)
                linkQueue.put(group)
            }
        }

        done = true
        reader.close()
    }

    private fun nextGroupLinks(): List<Link> {
        val links = ArrayList<Link>()
        links.add(firstGroupLink)

        val currentGroupingColumnValue = nextGroupingColumnValue
        while (reader.hasNext()) {
            val line = reader.next()
            if (line.isEmpty()) {
                endOfLinks = true
                break
            }

            val link = parseLink(line)
            if (link[groupingColumn] == currentGroupingColumnValue) {
                links.add(link)
            } else {
                firstGroupLink = link
                nextGroupingColumnValue = link[groupingColumn]
                break
            }
        }

        lastGroupingColumnValue = currentGroupingColumnValue
        if (!reader.hasNext())
            endOfLinks = true

        return links
    }

    private fun parseLink(raw: String): Link {
        val delimiter = raw.indexOf(',')
        return Link(raw.substring(0, delimiter).toInt(), raw.substring(delimiter + 1, raw.length).toInt())
    }
}
