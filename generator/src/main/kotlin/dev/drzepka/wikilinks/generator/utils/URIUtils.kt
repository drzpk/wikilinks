package dev.drzepka.wikilinks.generator.utils

import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun URI.getFilePath(): File {
    if (scheme != "file")
        throw IllegalArgumentException("Expected file URI scheme")

    if (!host.isNullOrBlank() || path.isNullOrBlank())
        throw IllegalArgumentException("URI with the file scheme should start with 3 or more forward slashes")

    // Remove leading slash from the path
    return File(path.substring(1))
}

fun URI.isQueryParamTrue(name: String): Boolean = getQueryParamValue(name)?.lowercase() == "true"

fun URI.getQueryParamValue(name: String): String? {
    val query = this.rawQuery ?: ""
    return query.split('&')
        .firstOrNull { it.startsWith("$name=") }
        ?.let { URLDecoder.decode(it.substringAfter('='), StandardCharsets.UTF_8) }
}
