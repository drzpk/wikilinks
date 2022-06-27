package dev.drzepka.wikilinks.app.config

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun getEnvironmentVariable(name: String): String? = getenv(name)?.toKString()
