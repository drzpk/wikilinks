package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.front.model.PageHint
import kotlinx.browser.window
import kotlin.js.Promise
import kotlin.random.Random

object MockPageSearchService : PageSearchService {

    override fun search(title: String): Promise<List<PageHint>> {
        val hints = (0 until  Random.nextInt(2, 5)).map {
            PageHint(
                Random.nextInt(),
                "$title ${Random.nextInt(1000)}",
                "This is a description for $title ${Random.nextInt(10000, 10000000)}",
                null
            )
        }

        return Promise { resolve, _ ->
            window.setTimeout({
                resolve.invoke(hints)
            }, Random.nextInt(800, 1500))
        }
    }
}
