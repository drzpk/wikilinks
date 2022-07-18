package dev.drzepka.wikilinks.app.utils

import kotlin.native.ref.WeakReference

actual class MultiplatformWeakReference<T : Any> actual constructor(value: T) {
    private val ref = WeakReference(value)

    actual fun getValue(): T? = ref.value
}
