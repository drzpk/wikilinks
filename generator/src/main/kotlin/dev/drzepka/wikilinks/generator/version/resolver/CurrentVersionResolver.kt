package dev.drzepka.wikilinks.generator.version.resolver

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

interface CurrentVersionResolver {
    fun resolve(language: DumpLanguage): String?
}
