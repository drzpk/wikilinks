package dev.drzepka.wikilinks.app.config

actual fun getEnvironmentVariable(name: String): String? = System.getenv(name)
