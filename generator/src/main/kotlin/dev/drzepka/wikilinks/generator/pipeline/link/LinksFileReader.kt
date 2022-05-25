package dev.drzepka.wikilinks.generator.pipeline.link

import dev.drzepka.wikilinks.generator.model.Link
import dev.drzepka.wikilinks.generator.pipeline.reader.Reader
import java.io.Closeable

class LinksFileReader(private val reader: Reader, val groupingColumn: Int) : Iterator<List<Link>>, Closeable {
    private var firstGroupLink: Link
    private var endOfLinks = false

    var nextGroupingColumnValue: Int
        private set

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

    override fun hasNext(): Boolean = !endOfLinks && reader.hasNext()

    override fun next(): List<Link> {
        val links = ArrayList<Link>()
        links.add(firstGroupLink)

        while (reader.hasNext()) {
            val line = reader.next()
            if (line.isEmpty()) {
                endOfLinks = true
                break
            }

            val link = parseLink(line)
            if (link[groupingColumn] == nextGroupingColumnValue) {
                links.add(link)
            } else {
                firstGroupLink = link
                nextGroupingColumnValue = link[groupingColumn]
                break
            }
        }

        return links
    }

    override fun close() {
        reader.close()
    }

    private fun parseLink(raw: String): Link {
        val delimiter = raw.indexOf(',')
        return Link(raw.substring(0, delimiter).toInt(), raw.substring(delimiter + 1, raw.length).toInt())
    }
}
