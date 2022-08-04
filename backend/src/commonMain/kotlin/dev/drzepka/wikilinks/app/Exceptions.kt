package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

class LanguageNotAvailableException(val language: DumpLanguage) : RuntimeException()
