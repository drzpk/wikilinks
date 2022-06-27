package dev.drzepka.wikilinks.generator.model

data class ResolvedDump(val url: String, val size: Long) {
    val fileName: String
        get() = url.substringAfterLast('/')
}
