package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.front.model.PageHint
import kotlinx.browser.window
import kotlin.js.Promise
import kotlin.random.Random

object MockPageSearchService : PageSearchService {

    override fun search(language: DumpLanguage, title: String, exact: Boolean): Promise<List<PageHint>> {
        val hintCount = if (!exact) Random.nextInt(2, 5) else 1
        val hints = (0 until hintCount).map {
            val suffix = if (it == 0) "" else " $it"
            PageHint(
                Random.nextInt(),
                "$title$suffix",
                "This is a description for $title ${Random.nextInt(10000, 10000000)}",
                null
            )
        }

        return Promise { resolve, _ ->
            window.setTimeout({
                resolve.invoke(hints)
            }, Random.nextInt(400, 1000))
        }
    }
}
