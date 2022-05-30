package dev.drzepka.wikilinks.app

enum class Environment {
    JVM, LINUX
}

expect val environment: Environment

expect fun exit(status: Int): Nothing
