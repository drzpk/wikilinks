package dev.drzepka.wikilinks.front.service

import dev.drzepka.wikilinks.front.model.PageHint
import kotlin.random.Random

object PageSearchService {

    fun query(title: String): List<PageHint> {
        // TODO: real implementation

        return (0 until  Random.nextInt(2, 5)).map {
            PageHint(
                Random.nextInt(),
                "$title ${Random.nextInt(1000)}",
                "This is a description for $title ${Random.nextInt(10000, 10000000)}"
            )
        }
    }
}
