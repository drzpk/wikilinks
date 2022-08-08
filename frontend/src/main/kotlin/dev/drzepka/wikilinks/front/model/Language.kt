package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

fun DumpLanguage.displayName(): String = when (this) {
    DumpLanguage.EN -> "English"
    DumpLanguage.PL -> "Polish"
    DumpLanguage.DE -> "German"
    DumpLanguage.FR -> "French"
    DumpLanguage.ES -> "Spanish"
    DumpLanguage.SV -> "Swedish"
    DumpLanguage.NL -> "Dutch"
    DumpLanguage.IT -> "Italian"
    DumpLanguage.JA -> "Japanese"
    DumpLanguage.PT -> "Portuguese"
}

fun DumpLanguage.flagCss(): String {
    val countryCode = when (this) {
        DumpLanguage.EN -> "gb"
        DumpLanguage.JA -> "jp"
        else -> value
    }
    return "fi fi-$countryCode"
}
