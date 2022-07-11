package dev.drzepka.wikilinks.app.model

import io.ktor.http.*

data class Resource(val contentType: ContentType, val content: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Resource

        if (contentType != other.contentType) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentType.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}
