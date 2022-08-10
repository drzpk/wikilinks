package dev.drzepka.wikilinks.common.utils

fun sanitizePageTitle(title: String): String = title.replace(" ", "_")
