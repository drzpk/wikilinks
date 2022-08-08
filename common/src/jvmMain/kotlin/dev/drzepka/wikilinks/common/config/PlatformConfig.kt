package dev.drzepka.wikilinks.common.config

actual fun getEnvironmentVariable(name: String): String? = System.getenv(name)
