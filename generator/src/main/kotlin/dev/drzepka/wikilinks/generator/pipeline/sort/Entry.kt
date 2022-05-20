package dev.drzepka.wikilinks.generator.pipeline.sort

data class Entry(val sortValue: Int, val line: String) : Comparable<Entry> {
    override fun compareTo(other: Entry): Int = sortValue.compareTo(other.sortValue)
}
