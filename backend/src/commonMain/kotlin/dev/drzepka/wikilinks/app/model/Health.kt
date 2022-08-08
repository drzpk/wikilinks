package dev.drzepka.wikilinks.app.model

@kotlinx.serialization.Serializable
data class Health(
    val healthy: Boolean,
    val message: String?,
    val appVersion: String,
    val uptimeSeconds: Int
)
