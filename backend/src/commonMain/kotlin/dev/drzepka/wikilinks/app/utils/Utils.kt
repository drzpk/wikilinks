package dev.drzepka.wikilinks.app.utils

enum class Environment {
    JVM, LINUX
}

expect val environment: Environment

expect fun exit(status: Int): Nothing

expect fun absolutePath(path: String): String
