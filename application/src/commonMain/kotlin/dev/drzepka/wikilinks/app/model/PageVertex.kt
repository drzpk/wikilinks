package dev.drzepka.wikilinks.app.model

data class PageVertex(val page: Int, private val parent: PageVertex?) {
    val depth: Int = (parent?.depth ?: -1) + 1

    fun unfold(): Path {
        val size = depth + 1
        val pages = IntArray(size)
        var index = size - 1
        var current: PageVertex? = this

        while (current != null) {
            pages[index--] = current.page
            current = current.parent
        }

        return Path(*pages)
    }

    override fun toString(): String {
        return "PageVertex(depth: $depth, page: $page, parent: $parent)"
    }
}
