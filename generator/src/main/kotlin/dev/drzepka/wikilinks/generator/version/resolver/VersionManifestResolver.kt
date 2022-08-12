package dev.drzepka.wikilinks.generator.version.resolver

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import java.io.File
import java.util.*

class VersionManifestResolver(workingDirectory: File) : CurrentVersionResolver {
    private val file = File(workingDirectory, "version-manifest.properties")
    private var properties = Properties().also { props ->
        if (file.isFile) {
            file.reader().use {
                props.load(it)
            }
        }
    }

    override fun resolve(language: DumpLanguage): String? = properties[language.value] as String?

    fun setVersion(language: DumpLanguage, version: String) {
        properties.setProperty(language.value, version)
        file.writer().use {
            properties.store(it, null)
        }
    }
}
