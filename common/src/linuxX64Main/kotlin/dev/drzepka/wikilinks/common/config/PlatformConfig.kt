package dev.drzepka.wikilinks.common.config

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun getEnvironmentVariable(name: String): String? = getenv(name)?.toKString()
