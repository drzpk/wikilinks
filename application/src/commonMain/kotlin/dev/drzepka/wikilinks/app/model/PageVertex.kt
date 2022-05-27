package dev.drzepka.wikilinks.app.model

/**
 * A vertex of page links graph that only keeps references to the parents with the lowest possible depth.
 *
 * @param root if set to true,
 */
data class PageVertex(val page: Int, private val root: Boolean = true) {
    private val parents = mutableListOf<PageVertex>()
    private var depth = 0

    constructor(page: Int, parent: PageVertex) : this(page, false) {
        addParent(parent)
    }

    constructor(page: Int, parents: List<PageVertex>) : this(page, false) {
        parents.forEach { addParent(it) }
    }

    fun addParent(parent: PageVertex) {
        // Depth of 0 means this vertex has no parents
        if (parent.depth + 1 < depth || depth == 0) {
            // There exists a shorter path
            parents.clear()
            depth = parent.depth + 1
        }

        if (parent.depth + 1 == depth)
            parents.add(parent)
    }

    fun unfold(): List<Path> {
        val paths = unfold(arrayListOf())
        return paths.map { Path(it) }
    }

    private fun unfold(partialPath: MutableList<Int>): List<MutableList<Int>> {
        partialPath.add(0, page)

        val unfolded = ArrayList<MutableList<Int>>()
        if (parents.isNotEmpty()) {
            parents.forEach {
                val copy = partialPath.toMutableList()
                unfolded.addAll(it.unfold(copy))
            }

        } else {
            unfolded.add(partialPath)
        }

        return unfolded
    }

    override fun toString(): String {
        val parentPages = parents.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.page.toString() }
        return "PageVertex(page: $page, parents: $parentPages)"
    }
}
