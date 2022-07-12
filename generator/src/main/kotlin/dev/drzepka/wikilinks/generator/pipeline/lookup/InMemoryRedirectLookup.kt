package dev.drzepka.wikilinks.generator.pipeline.lookup

class InMemoryRedirectLookup : RedirectLookup {
    private val redirects = hashMapOf<Int, Int>()

    override fun get(from: Int): Int? {
        var target = redirects[from] ?: return null
        val visited = mutableSetOf<Int>()
        visited.add(from)
        visited.add(target)

        while (true) {
            val next = redirects[target] ?: return target
            if (next in visited)
                return null

            visited.add(next)
            target = next
        }
    }

    override fun set(from: Int, to: Int) {
        redirects[from] = to
    }

    override fun clear() {
        redirects.clear()
    }
}
