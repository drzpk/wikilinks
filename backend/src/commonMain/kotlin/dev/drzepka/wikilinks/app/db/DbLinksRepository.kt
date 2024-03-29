package dev.drzepka.wikilinks.app.db

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.app.model.Link
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlin.math.ceil

class DbLinksRepository(private val registry: DatabaseRegistry) : LinksRepository {

    override suspend fun getInLinksCount(language: DumpLanguage, vararg pageIds: Int): Int {
        val database = registry.getLinksDatabase(language)
        return splitQuery(pageIds) {
            database.linksQueries.countInLinks(it).executeAsOneOrNull()?.SUM?.toInt() ?: 0
        }.sum()
    }

    override suspend fun getOutLinksCount(language: DumpLanguage, vararg pageIds: Int): Int {
        val database = registry.getLinksDatabase(language)
        return splitQuery(pageIds) {
            database.linksQueries.countOutLinks(it).executeAsOneOrNull()?.SUM?.toInt() ?: 0
        }.sum()
    }

    override suspend fun getInLinks(language: DumpLanguage, vararg pageIds: Int): List<Link> {
        val database = registry.getLinksDatabase(language)
        return splitQuery(pageIds) { database.linksQueries.getInLinks(it).executeAsList() }
            .flatMap { it }
            .flatMap { splitInLinks(it.in_links, it.page_id.toInt()) }
            .toList()
    }

    override suspend fun getOutLinks(language: DumpLanguage, vararg pageIds: Int): List<Link> {
        val database = registry.getLinksDatabase(language)
        return splitQuery(pageIds) { database.linksQueries.getOutLinks(it).executeAsList() }
            .flatMap { it }
            .flatMap { splitOutLinks(it.page_id.toInt(), it.out_links) }
            .toList()
    }

    private fun <T> splitQuery(values: IntArray, handler: (section: List<Long>) -> T): Sequence<T> {
        val longValues = values.map { it.toLong() }
        return if (longValues.size <= PARAMETER_SIZE_LIMIT) {
            sequenceOf(handler.invoke(longValues))
        } else {
            val sectionCount = ceil(longValues.size.toDouble() / PARAMETER_SIZE_LIMIT).toInt()
            return (0 until sectionCount)
                .asSequence()
                .map { sectionNo ->
                    val endIndex = ((sectionNo + 1) * PARAMETER_SIZE_LIMIT).coerceAtMost(longValues.size)
                    val section = longValues.subList(sectionNo * PARAMETER_SIZE_LIMIT, endIndex)
                    handler.invoke(section)
                }
        }
    }

    private fun splitInLinks(raw: String, to: Int): List<Link> = splitLinks(raw).map { Link(it, to) }

    private fun splitOutLinks(from: Int, raw: String): List<Link> = splitLinks(raw).map { Link(from, it) }

    private fun splitLinks(raw: String): List<Int> {
        if (raw.isEmpty())
            return emptyList()

        return raw.split(',').map { it.toInt() }
    }

    companion object {
        // https://www.sqlite.org/limits.html, 9th limit
        private const val PARAMETER_SIZE_LIMIT = 32766
    }
}
