package dev.drzepka.wikilinks.app.service

import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.app.model.Resource
import dev.drzepka.wikilinks.app.utils.absolutePath
import dev.drzepka.wikilinks.common.utils.MultiplatformFile
import io.ktor.http.*

class FrontendResourceService {
    private val rootPath = absolutePath(Configuration.frontendResourcesDirectory!!)

    fun getResource(path: String): Resource? {
        val resolved = absolutePath("$rootPath/$path")
        if (!resolved.startsWith(rootPath))
            return null

        val file = MultiplatformFile(resolved)
        if (!file.isFile())
            return null

        val content = file.readBytes()
        return Resource(getContentType(path.substringAfterLast(".", missingDelimiterValue = "")), content)
    }

    private fun getContentType(extension: String): ContentType = when (extension.lowercase()) {
        "html" -> ContentType.Text.Html
        "css" -> ContentType.Text.CSS
        "js" -> ContentType.Text.JavaScript
        "jpg", "jpeg" -> ContentType.Image.JPEG
        "png" -> ContentType.Image.PNG
        else -> ContentType.Text.Plain
    }
}
