package dev.drzepka.wikilinks.app.utils

import java.lang.ref.WeakReference

actual class MultiplatformWeakReference<T : Any> actual constructor(value: T) {
    private val ref = WeakReference(value)

    actual fun getValue(): T? = ref.get()
}
