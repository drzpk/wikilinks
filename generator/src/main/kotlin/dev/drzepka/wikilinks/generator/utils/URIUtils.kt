package dev.drzepka.wikilinks.generator.utils

import java.io.File
import java.net.URI

fun URI.getFilePath(): File {
    if (scheme != "file")
        throw IllegalArgumentException("Expected file URI scheme")

    if (!host.isNullOrBlank() || path.isNullOrBlank())
        throw IllegalArgumentException("URI with the file scheme should start with 3 or more forward slashes")

    // Remove leading slash from the path
    return File(path.substring(1))
}
