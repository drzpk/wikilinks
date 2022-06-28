package dev.drzepka.wikilinks.common.model.dump

data class ArchiveDump(val url: String, val size: Long, val supportsHttpRange: Boolean) {
    val fileName: String
        get() = url.substringAfterLast('/')
}
