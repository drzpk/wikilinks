package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

fun DumpLanguage.displayName(): String = when (this) {
    DumpLanguage.EN -> "English"
    DumpLanguage.PL -> "Polish"
}

fun DumpLanguage.flagCss(): String {
    val countryCode = when (this) {
        DumpLanguage.EN -> "gb"
        else -> value
    }
    return "fi fi-$countryCode"
}
