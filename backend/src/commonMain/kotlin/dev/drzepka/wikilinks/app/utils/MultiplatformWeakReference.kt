package dev.drzepka.wikilinks.app.utils

expect class MultiplatformWeakReference<T : Any>(value: T) {
    fun getValue(): T?
}
